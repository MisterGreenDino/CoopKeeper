package org.bot.eventTracker;

import java.time.Instant;

/** Computes the next real-world trigger instant for a given event, based on its {@link EventType}. */
public final class EventScheduler {

    private EventScheduler() {
    }

    /** Next occurrence after `after`, for any event type. `customPeriodMs` is only used for CUSTOM. */
    public static Instant nextTrigger(EventType type, Instant after, long customPeriodMs) {
        return switch (type.kind) {
            case FIXED_PERIOD -> SkyBlockCalendar.nextAnchored(after, type.periodMs, type.anchorOffsetMs);
            case CALENDAR -> SkyBlockCalendar.nextOccurrenceOfDate(after, type.monthIndex, type.startDay);
            case CUSTOM -> SkyBlockCalendar.nextAnchored(after, Math.max(customPeriodMs, 1000L));
        };
    }
}
