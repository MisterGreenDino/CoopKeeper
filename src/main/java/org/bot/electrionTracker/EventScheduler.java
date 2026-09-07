package org.bot.eventTracker;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/** Pure time-math helpers for figuring out when recurring events next fire. */
public final class EventScheduler {

    private EventScheduler() {
    }

    /** Next top-of-the-hour instant (used for Dark Auction, which starts on the real-world hour). */
    public static Instant nextTopOfHour() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime next = now.withMinute(0).withSecond(0).withNano(0).plusHours(1);
        return next.toInstant();
    }

    /** Next occurrence given a fixed period, anchored to `anchor` (e.g. epoch or event creation time). */
    public static Instant nextOccurrence(Instant anchor, long periodMs) {
        long now = System.currentTimeMillis();
        long anchorMs = anchor.toEpochMilli();
        if (periodMs <= 0) {
            return anchor;
        }
        long elapsedPeriods = Math.max(0, (now - anchorMs) / periodMs + 1);
        return Instant.ofEpochMilli(anchorMs + elapsedPeriods * periodMs);
    }
}
