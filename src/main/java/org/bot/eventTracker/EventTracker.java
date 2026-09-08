package org.bot.eventTracker;

import java.awt.Color;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bot.Bot;
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
        for (EventType type : EnumSet.complementOf(EnumSet.of(EventType.CUSTOM))) {
            track(type, channelId);
        }
        return getAll();
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

        if (event.type.kind == EventType.Kind.CALENDAR && event.type.durationSkyblockDays > 0) {
            eb.addField("Duration", event.type.durationSkyblockDays + " SkyBlock day(s) (~" +
                    (event.type.durationSkyblockDays * SkyBlockCalendar.DAY_MS / 60000) + " real minutes)", false);
        }

        channel.sendMessage("@everyone")
                .setAllowedMentions(EnumSet.of(MentionType.EVERYONE))
                .setEmbeds(eb.build())
                .queue();
    }
}
