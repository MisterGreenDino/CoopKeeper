package org.bot.eventTracker;

import java.time.Instant;
import java.util.UUID;

/** A tracked recurring event, with the channel it should be announced in and its next trigger time. */
public class SkyblockEvent {

    public final String id;
    public final EventType type;
    public final String label;
    public final String channelId;
    public final long periodMs;
    public Instant nextTrigger;

    public SkyblockEvent(EventType type, String label, String channelId, long periodMs, Instant nextTrigger) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.label = label;
        this.channelId = channelId;
        this.periodMs = periodMs;
        this.nextTrigger = nextTrigger;
    }
}
