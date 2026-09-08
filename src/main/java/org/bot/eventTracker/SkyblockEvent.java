package org.bot.eventTracker;

import java.time.Instant;
import java.util.UUID;

/** A tracked event, with the channel it should be announced in and its next trigger time. */
public class SkyblockEvent {

    public final String id;
    public final EventType type;
    public final String label;
    public final String channelId;
    /** Only used for CUSTOM events; calendar/fixed-period events recompute via EventScheduler. */
    public final long customPeriodMs;
    public Instant nextTrigger;

    public SkyblockEvent(EventType type, String label, String channelId, long customPeriodMs, Instant nextTrigger) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.label = label;
        this.channelId = channelId;
        this.customPeriodMs = customPeriodMs;
        this.nextTrigger = nextTrigger;
    }

    /** When this occurrence ends, if it's a CALENDAR event with a known duration. Null otherwise. */
    public Instant endsAt() {
        if (type.kind == EventType.Kind.CALENDAR && type.durationSkyblockDays > 0) {
            return nextTrigger.plusMillis(type.durationSkyblockDays * SkyBlockCalendar.DAY_MS);
        }
        return null;
    }
}
