package org.bot.eventTracker;

/**
 * Known recurring SkyBlock events.
 *
 * IMPORTANT: the official Hypixel Public API (https://api.hypixel.net/) does NOT expose a
 * calendar/events endpoint - only /resources/skyblock/{collections,skills,items,election,bingo}.
 * So every calendar-based event below is computed purely from SkyBlock's day/month/year math via
 * {@link SkyBlockCalendar} - the same approach every community SkyBlock calendar tool uses, since
 * the game client does the same local computation.
 *
 * Confidence notes (checked against the SkyBlock Wiki and cross-verified against real timestamps
 * on 2026-09-08, see repo history):
 *  - DARK_AUCTION, JACOBS_CONTEST: wiki-documented fixed real-time periods, anchored to the
 *    official SkyBlock epoch. High confidence.
 *  - SPOOKY_FESTIVAL, FEAR_MONGERER, HARVEST_FEAST, TRAVELING_ZOO, JERRYS_WORKSHOP_OPENS,
 *    NEW_YEAR: wiki-documented calendar dates, cross-checked against live timestamps. High
 *    confidence.
 *  - SEASON_OF_JERRY_EVENT: single cross-checked data point (Late Winter 24th) for the "Mount
 *    Jerry erupts" bonus event inside Jerry's Workshop. Reasonable confidence, but only one
 *    sample - if it turns out to fire more than once per SkyBlock year, this will need updating.
 *
 * Deliberately NOT included (schedule not confirmed at the time this was written - verify before
 * adding, a wrong date here means a false @everyone ping): Mining Fiesta, Fetchur's daily item,
 * Cult of the Fallen Star, the 12-year rotating "Year of the X" events, and anything tied to a
 * specific mayor's perk (those depend on who's elected - see ElectionTracker/MayorWatcher).
 */
public enum EventType {

    DARK_AUCTION(
            "\ud83d\udd28 Dark Auction",
            Kind.FIXED_PERIOD,
            3L * SkyBlockCalendar.DAY_MS, 0L,
            -1, -1, -1
    ),

    JACOBS_CONTEST(
            "\ud83c\udf3e Jacob's Farming Contest",
            Kind.FIXED_PERIOD,
            3L * SkyBlockCalendar.DAY_MS, SkyBlockCalendar.DAY_MS, // starts 1 SkyBlock day after each Dark Auction slot
            -1, -1, -1
    ),

    NEW_YEAR(
            "\ud83c\udf86 New Year Celebration",
            Kind.CALENDAR,
            -1, -1,
            11, 29, // Late Winter 29th - runs into the new SkyBlock year
            3
    ),

    TRAVELING_ZOO(
            "\ud83e\udd81 Traveling Zoo",
            Kind.CALENDAR,
            -1, -1,
            3, 1, // Early Summer 1st
            3
    ),

    FEAR_MONGERER(
            "\ud83c\udf83 Fear Mongerer arrives (Spooky decorations)",
            Kind.CALENDAR,
            -1, -1,
            7, 26, // Autumn 26th
            9
    ),

    SPOOKY_FESTIVAL(
            "\ud83c\udf83 Spooky Festival",
            Kind.CALENDAR,
            -1, -1,
            7, 29, // Autumn 29th
            3
    ),

    JERRYS_WORKSHOP_OPENS(
            "\u2744\ufe0f Jerry's Workshop opens (Late Winter)",
            Kind.CALENDAR,
            -1, -1,
            11, 1, // Late Winter 1st, the whole month
            31
    ),

    SEASON_OF_JERRY_EVENT(
            "\ud83c\udf0b Season of Jerry (Mount Jerry erupts)",
            Kind.CALENDAR,
            -1, -1,
            11, 24, // Late Winter 24th
            3
    ),

    HARVEST_FEAST(
            "\ud83c\udf3d Harvest Feast begins",
            Kind.CALENDAR,
            -1, -1,
            6, 1, // Early Autumn 1st, runs the whole autumn season
            93
    ),

    CUSTOM("\ud83d\udcc5 Custom Event", Kind.CUSTOM, -1, -1, -1, -1, -1);

    public enum Kind {
        FIXED_PERIOD, CALENDAR, CUSTOM
    }

    public final String displayName;
    public final Kind kind;
    /** Only meaningful for FIXED_PERIOD. */
    public final long periodMs;
    /** Only meaningful for FIXED_PERIOD: offset added to the SkyBlock epoch before repeating. */
    public final long anchorOffsetMs;
    /** Only meaningful for CALENDAR: 0-11. */
    public final int monthIndex;
    /** Only meaningful for CALENDAR: 1-31. */
    public final int startDay;
    /** Only meaningful for CALENDAR: how many SkyBlock days the event runs for. */
    public final int durationSkyblockDays;

    EventType(String displayName, Kind kind, long periodMs, long anchorOffsetMs,
              int monthIndex, int startDay, int durationSkyblockDays) {
        this.displayName = displayName;
        this.kind = kind;
        this.periodMs = periodMs;
        this.anchorOffsetMs = anchorOffsetMs;
        this.monthIndex = monthIndex;
        this.startDay = startDay;
        this.durationSkyblockDays = durationSkyblockDays;
    }
}
