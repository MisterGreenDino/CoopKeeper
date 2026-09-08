package org.bot.electrionTracker;

import java.awt.Color;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bot.Bot;
import org.bot.utils.ChannelMessageTracker;
import org.bot.utils.Log;

/**
 * Polls {@link ElectionTracker} for the current mayor and announces (embed + @everyone) in every
 * subscribed channel whenever the mayor changes. ElectionTracker already caches the underlying
 * API call for 5 minutes, so polling here is cheap.
 */
public final class MayorWatcher {

    private static final Set<String> channels = ConcurrentHashMap.newKeySet();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static volatile String lastMayorKey;
    private static volatile boolean initialized = false;

    private MayorWatcher() {
    }

    static {
        scheduler.scheduleAtFixedRate(MayorWatcher::tick, 0L, 5L, TimeUnit.MINUTES);
    }

    public static void track(String channelId) {
        channels.add(channelId);
    }

    public static void untrack(String channelId) {
        channels.remove(channelId);
        String trackerKey = "mayor:" + channelId;
        if (Bot.jda != null) {
            TextChannel channel = Bot.jda.getTextChannelById(channelId);
            if (channel != null) {
                ChannelMessageTracker.deletePrevious(channel, trackerKey);
            }
        }
        ChannelMessageTracker.forget(trackerKey);
    }

    public static Set<String> getTrackedChannels() {
        return channels;
    }

    private static void tick() {
        try {
            Mayor mayor = ElectionTracker.getCurrentMayor();
            if (mayor == null || mayor.key == null) {
                return;
            }

            if (!initialized) {
                // First run after startup: just remember the current mayor, don't announce
                // (otherwise every restart would falsely announce "a new mayor" every time).
                lastMayorKey = mayor.key;
                initialized = true;
                return;
            }

            if (!mayor.key.equals(lastMayorKey)) {
                lastMayorKey = mayor.key;
                announce(mayor);
            }
        } catch (Exception e) {
            Log.error("Mayor watcher tick failed: " + e.getMessage());
        }
    }

    private static void announce(Mayor mayor) {
        if (Bot.jda == null || channels.isEmpty()) {
            return;
        }

        String perks = mayor.perks.isEmpty() ? "None" : perkNames(mayor.perks);
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("\ud83d\uddf3\ufe0f New SkyBlock Mayor: " + mayor.name)
                .setColor(Color.MAGENTA)
                .addField("Active Perks", perks, false);
        if (mayor.minister != null) {
            eb.addField("Minister", mayor.minister, false);
        }

        for (String channelId : channels) {
            TextChannel channel = Bot.jda.getTextChannelById(channelId);
            if (channel == null) {
                Log.warn("Mayor watcher channel not found: " + channelId);
                continue;
            }
            String trackerKey = "mayor:" + channelId;
            ChannelMessageTracker.deletePrevious(channel, trackerKey);
            channel.sendMessage("@everyone")
                    .setAllowedMentions(EnumSet.of(MentionType.EVERYONE))
                    .setEmbeds(eb.build())
                    .queue(message -> ChannelMessageTracker.remember(trackerKey, message.getId()));
        }
    }

    private static String perkNames(java.util.List<Perk> perks) {
        StringBuilder sb = new StringBuilder();
        for (Perk perk : perks) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(perk.name);
        }
        return sb.toString();
    }
}
