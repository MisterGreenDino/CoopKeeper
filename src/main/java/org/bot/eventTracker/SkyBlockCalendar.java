package org.bot.eventTracker;

import java.time.Instant;

/**
 * Converts between real-world time and SkyBlock's in-game calendar.
 *
 * Time units (confirmed on the SkyBlock Wiki "Time Systems" page):
 *   1 SkyBlock day   = 20 real minutes  = 1,200,000 ms
 *   1 SkyBlock month = 31 SkyBlock days = 37,200,000 ms
 *   1 SkyBlock year  = 12 months        = 446,400,000 ms (5 real days 4 hours)
 *
 * EPOCH_MS is the official value published on the Wiki: "The SkyBlock Epoch ... is defined to be
 * 1st of Early Spring, Year 1 at 00:00, which converts to Jun 11, 2019 at 17:55:00 (UTC), or unix
 * time of 1560275700." Since that instant is Year 1 (not Year 0), getYear() adds 1 to the raw
 * zero-indexed division. Cross-checked against several live event timestamps (Spooky Festival,
 * Harvest Feast, Dark Auction alignment) and they all land exactly where expected.
 */
public final class SkyBlockCalendar {

    public static final long EPOCH_MS = 1_560_275_700_000L;

    public static final long DAY_MS = 1_200_000L;
    public static final long MONTH_MS = 31L * DAY_MS;
    public static final long YEAR_MS = 12L * MONTH_MS;

    public static final String[] MONTH_NAMES = {
            "Early Spring", "Spring", "Late Spring",
            "Early Summer", "Summer", "Late Summer",
            "Early Autumn", "Autumn", "Late Autumn",
            "Early Winter", "Winter", "Late Winter"
    };

    private SkyBlockCalendar() {
    }

    private static long elapsed(Instant instant) {
        return instant.toEpochMilli() - EPOCH_MS;
    }

    public static long getYear(Instant instant) {
        return Math.floorDiv(elapsed(instant), YEAR_MS) + 1;
    }

    /** 0-11, index into {@link #MONTH_NAMES}. */
    public static int getMonthIndex(Instant instant) {
        return (int) Math.floorMod(Math.floorDiv(elapsed(instant), MONTH_MS), 12);
    }

    /** 1-31. */
    public static int getDay(Instant instant) {
        return (int) Math.floorMod(Math.floorDiv(elapsed(instant), DAY_MS), 31) + 1;
    }

    public static String getMonthName(Instant instant) {
        return MONTH_NAMES[getMonthIndex(instant)];
    }

    public static String describe(Instant instant) {
        return getMonthName(instant) + " " + getDay(instant) + ", Year " + getYear(instant);
    }

    /** The real-world instant at which the given SkyBlock (displayed year, monthIndex 0-11, day 1-31) begins. */
    public static Instant instantOf(long year, int monthIndex, int day) {
        long ms = EPOCH_MS + (year - 1) * YEAR_MS + monthIndex * MONTH_MS + (day - 1L) * DAY_MS;
        return Instant.ofEpochMilli(ms);
    }

    /**
     * Next upcoming real-world instant when SkyBlock reaches (monthIndex, day), searching forward
     * from `after`. Rolls over to the next SkyBlock year if this year's date has already passed.
     */
    public static Instant nextOccurrenceOfDate(Instant after, int monthIndex, int day) {
        long year = getYear(after);
        Instant candidate = instantOf(year, monthIndex, day);
        if (!candidate.isAfter(after)) {
            candidate = instantOf(year + 1, monthIndex, day);
        }
        return candidate;
    }

    /**
     * Next instant, after `after`, at which SkyBlock's daily clock rolls over to midnight (i.e. a
     * new SkyBlock day starts). Since a SkyBlock day is 1200 real seconds, this repeats every 20
     * real minutes, aligned to EPOCH_MS - not to real-world clock boundaries.
     */
    public static Instant nextDayBoundary(Instant after) {
        return nextAnchored(after, DAY_MS);
    }

    /**
     * Generic helper: the next instant after `after` that is EPOCH_MS + k*periodMs for some
     * integer k. Used for anything that repeats on a fixed SkyBlock-time-aligned schedule, e.g.
     * the Dark Auction (every 3 SkyBlock days = periodMs of 3 * DAY_MS = 3,600,000 ms).
     */
    public static Instant nextAnchored(Instant after, long periodMs) {
        return nextAnchored(after, periodMs, 0L);
    }

    /**
     * Same as {@link #nextAnchored(Instant, long)} but anchored to EPOCH_MS + anchorOffsetMs
     * instead of EPOCH_MS itself. Used for events that repeat on the same fixed schedule as
     * something anchored at the epoch, but shifted by a fixed offset - e.g. Jacob's Farming
     * Contest starts exactly one SkyBlock day (20 real minutes) after each Dark Auction slot.
     */
    public static Instant nextAnchored(Instant after, long periodMs, long anchorOffsetMs) {
        long base = EPOCH_MS + anchorOffsetMs;
        long sinceAnchor = after.toEpochMilli() - base;
        long steps = Math.floorDiv(sinceAnchor, periodMs) + 1;
        return Instant.ofEpochMilli(base + steps * periodMs);
    }
}
