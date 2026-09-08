package org.bot.eventTracker;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bot.Bot;
import org.bot.utils.ChannelMessageTracker;
import org.bot.utils.DiscordTimestamps;
import org.bot.utils.Log;

/** Tracks recurring SkyBlock events and pings a channel with an embed when each one fires. */
public final class EventTracker {

    private static final Map<String, SkyblockEvent> events = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private EventTracker() {
    }

    static {
        scheduler.scheduleAtFixedRate(EventTracker::tick, 0L, 10L, TimeUnit.SECONDS);
    }

    /** Registers a single known (non-CUSTOM) event type to be announced in a channel. Idempotent per type+channel. */
    public static SkyblockEvent track(EventType type, String channelId) {
        if (type == EventType.CUSTOM) {
            throw new IllegalArgumentException("Use trackCustom(...) for custom events");
        }
        String key = type.name() + ":" + channelId;
        Instant next = EventScheduler.nextTrigger(type, Instant.now(), -1);
        SkyblockEvent event = new SkyblockEvent(type, type.displayName, channelId, -1, next);
        events.put(key, event);
        return event;
    }

    /** Registers every known calendar/fixed-period event (everything except CUSTOM) in one channel. */
    public static Collection<SkyblockEvent> trackAll(String channelId) {
        List<SkyblockEvent> justRegistered = new ArrayList<>();
        for (EventType type : EnumSet.complementOf(EnumSet.of(EventType.CUSTOM))) {
            justRegistered.add(track(type, channelId));
        }
        return justRegistered;
    }

    /** Registers a custom recurring event, e.g. a guild-run event, with an arbitrary real-world period. */
    public static SkyblockEvent trackCustom(String label, String channelId, long periodMs) {
        SkyblockEvent event = new SkyblockEvent(EventType.CUSTOM, label, channelId, periodMs,
                EventScheduler.nextTrigger(EventType.CUSTOM, Instant.now(), periodMs));
        events.put(event.id, event);
        return event;
    }

    public static Collection<SkyblockEvent> getAll() {
        return events.values();
    }

    public static SkyblockEvent remove(String id) {
        return events.remove(id);
    }

    /** Stops tracking a single known event type in a channel, and cleans up its last announcement. */
    public static boolean untrack(EventType type, String channelId) {
        String key = type.name() + ":" + channelId;
        SkyblockEvent removed = events.remove(key);
        if (removed == null) {
            return false;
        }
        cleanupMessage(removed);
        return true;
    }

    /** Stops tracking every event in a channel (used by /events_untrack_all), cleaning up their messages. */
    public static int untrackAll(String channelId) {
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, SkyblockEvent> e : events.entrySet()) {
            if (e.getValue().channelId.equals(channelId)) {
                keysToRemove.add(e.getKey());
            }
        }
        for (String key : keysToRemove) {
            SkyblockEvent removed = events.remove(key);
            if (removed != null) {
                cleanupMessage(removed);
            }
        }
        return keysToRemove.size();
    }

    private static void cleanupMessage(SkyblockEvent event) {
        String trackerKey = "event:" + event.id;
        if (Bot.jda != null) {
            TextChannel channel = Bot.jda.getTextChannelById(event.channelId);
            if (channel != null) {
                ChannelMessageTracker.deletePrevious(channel, trackerKey);
            }
        }
        ChannelMessageTracker.forget(trackerKey);
    }

    /** Snapshot of everything currently tracked, for {@link org.bot.persistence.Persistence}. */
    public static List<org.bot.persistence.BotState.TrackedEvent> exportState() {
        List<org.bot.persistence.BotState.TrackedEvent> list = new ArrayList<>();
        for (SkyblockEvent e : events.values()) {
            list.add(new org.bot.persistence.BotState.TrackedEvent(e.type.name(), e.channelId, e.label, e.customPeriodMs));
        }
        return list;
    }

    /** Re-registers previously saved events. Next-trigger times are recomputed from now, not restored verbatim. */
    public static void restore(List<org.bot.persistence.BotState.TrackedEvent> saved) {
        for (org.bot.persistence.BotState.TrackedEvent p : saved) {
            try {
                EventType type = EventType.valueOf(p.type());
                if (type == EventType.CUSTOM) {
                    trackCustom(p.label(), p.channelId(), p.customPeriodMs());
                } else {
                    track(type, p.channelId());
                }
            } catch (Exception e) {
                Log.warn("Skipped restoring a saved event: " + e.getMessage());
            }
        }
    }

    private static void tick() {
        Instant now = Instant.now();
        for (SkyblockEvent event : events.values()) {
            if (!now.isBefore(event.nextTrigger)) {
                announce(event);
                event.nextTrigger = EventScheduler.nextTrigger(event.type, event.nextTrigger, event.customPeriodMs);
            }
        }
    }

    private static void announce(SkyblockEvent event) {
        if (Bot.jda == null) {
            return;
        }
        TextChannel channel = Bot.jda.getTextChannelById(event.channelId);
        if (channel == null) {
            Log.warn("Event channel not found for " + event.label + " (" + event.channelId + ")");
            return;
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(event.label + " is starting now!")
                .setColor(Color.ORANGE)
                .setFooter("SkyBlock time: " + SkyBlockCalendar.describe(Instant.now()));

        Instant endsAt = event.endsAt();
        if (endsAt != null) {
            eb.addField("Ends", DiscordTimestamps.relativeAndTime(endsAt), false);
        }

        String trackerKey = "event:" + event.id;
        ChannelMessageTracker.deletePrevious(channel, trackerKey);

        channel.sendMessage("@everyone")
                .setAllowedMentions(EnumSet.of(MentionType.EVERYONE))
                .setEmbeds(eb.build())
                .queue(message -> ChannelMessageTracker.remember(trackerKey, message.getId()));
    }
}
