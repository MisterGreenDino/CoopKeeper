package org.bot.eventTracker;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bot.Bot;
import org.bot.utils.Log;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Tracks recurring SkyBlock events (Dark Auction + any custom ones) and pings a channel when they fire. */
public final class EventTracker {

    private static final Map<String, SkyblockEvent> events = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private EventTracker() {
    }

    static {
        scheduler.scheduleAtFixedRate(EventTracker::tick, 0L, 10L, TimeUnit.SECONDS);
    }

    /** Registers the Dark Auction (idempotent - re-registering just resets the trigger). */
    public static SkyblockEvent trackDarkAuction(String channelId) {
        SkyblockEvent event = new SkyblockEvent(EventType.DARK_AUCTION, EventType.DARK_AUCTION.displayName,
                channelId, EventType.DARK_AUCTION.periodMs, EventScheduler.nextTopOfHour());
        events.put(event.id, event);
        return event;
    }

    /** Registers a custom recurring event, e.g. a guild-run event, with an arbitrary period. */
    public static SkyblockEvent trackCustom(String label, String channelId, long periodMs) {
        SkyblockEvent event = new SkyblockEvent(EventType.CUSTOM, label, channelId, periodMs,
                EventScheduler.nextOccurrence(Instant.now(), periodMs));
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
                event.nextTrigger = EventScheduler.nextOccurrence(event.nextTrigger, event.periodMs);
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
        channel.sendMessage("\ud83d\udcc5 **" + event.label + "** is starting now!").queue();
    }
}
