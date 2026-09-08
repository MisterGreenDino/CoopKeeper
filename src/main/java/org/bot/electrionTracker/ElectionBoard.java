package org.bot.electrionTracker;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bot.Bot;
import org.bot.utils.ChannelMessageTracker;
import org.bot.utils.Log;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A persistent, auto-updating "Mayor Election" panel per channel (same content as /election,
 * including the vote chart), refreshed every 5 minutes - matching ElectionTracker's own cache
 * TTL, so there's no point checking more often. Unlike PlayerTracker/ReminderMenu, this deletes
 * and reposts on every refresh rather than editing in place, since the vote chart is a fresh
 * image attachment each time and Discord doesn't cleanly support swapping attachments on an
 * edited message.
 */
public final class ElectionBoard {

    private static final long REFRESH_INTERVAL_MINUTES = 5L;

    private static final Set<String> channels = ConcurrentHashMap.newKeySet();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private ElectionBoard() {
    }

    static {
        scheduler.scheduleAtFixedRate(ElectionBoard::tick, REFRESH_INTERVAL_MINUTES, REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    private static String trackerKey(String channelId) {
        return "election:" + channelId;
    }

    public static void track(String channelId) {
        channels.add(channelId);
        scheduler.execute(() -> refresh(channelId));
    }

    public static void untrack(String channelId) {
        channels.remove(channelId);
        if (Bot.jda != null) {
            TextChannel channel = Bot.jda.getTextChannelById(channelId);
            if (channel != null) {
                ChannelMessageTracker.deletePrevious(channel, trackerKey(channelId));
            }
        }
        ChannelMessageTracker.forget(trackerKey(channelId));
    }

    public static Set<String> getTrackedChannels() {
        return channels;
    }

    /** Deletes any stale panel left over from before a restart, then posts a fresh one. */
    public static void hardResetAllOnStartup() {
        for (String channelId : channels) {
            scheduler.execute(() -> refresh(channelId));
        }
    }

    private static void tick() {
        for (String channelId : channels) {
            try {
                refresh(channelId);
            } catch (Exception e) {
                Log.error("Election board tick failed for channel " + channelId + ": " + e.getMessage());
            }
        }
    }

    private static void refresh(String channelId) {
        if (Bot.jda == null) {
            return;
        }
        TextChannel channel = Bot.jda.getTextChannelById(channelId);
        if (channel == null) {
            return;
        }

        String trackerKey = trackerKey(channelId);
        try {
            Mayor mayor = ElectionTracker.getCurrentMayor();
            Election election = ElectionTracker.getOngoingElection();
            EmbedBuilder eb = ElectionEmbed.build(mayor, election);

            ChannelMessageTracker.deletePrevious(channel, trackerKey);

            if (election != null && !election.candidates.isEmpty()) {
                try {
                    byte[] chart = VoteChartGenerator.renderVotes(election.candidates);
                    eb.setImage("attachment://votes.png");
                    Message sent = channel.sendMessageEmbeds(eb.build())
                            .addFiles(FileUpload.fromData(chart, "votes.png"))
                            .complete();
                    ChannelMessageTracker.remember(trackerKey, sent.getId());
                    return;
                } catch (Exception chartError) {
                    Log.error("Election board chart render failed, posting without it: " + chartError.getMessage());
                }
            }

            Message sent = channel.sendMessageEmbeds(eb.build()).complete();
            ChannelMessageTracker.remember(trackerKey, sent.getId());
        } catch (Exception e) {
            Log.error("Failed to refresh election board for channel " + channelId + ": " + e.getMessage());
        }
    }
}
