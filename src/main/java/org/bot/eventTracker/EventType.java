package org.bot.eventTracker;

/**
 * Known recurring SkyBlock events.
 *
 * IMPORTANT: the official Hypixel Public API (https://api.hypixel.net/) does NOT expose a
 * calendar/events endpoint - only /resources/skyblock/{collections,skills,items,election,bingo}.
 * Checked against both the live docs and the HypixelDev/PublicAPI reference implementation.
 * So calendar-based events below (everything except DARK_AUCTION and CUSTOM) are computed purely
 * from SkyBlock's day/month/year math via {@link SkyBlockCalendar} - the same approach every
 * community SkyBlock calendar tool uses, since the game client does the same local computation.
 *
 * Each entry is either:
 *  - FIXED_PERIOD: repeats every `periodMs`, aligned to {@link SkyBlockCalendar#EPOCH_MS}
 *    (e.g. Dark Auction, every 3 SkyBlock days = every real hour).
 *  - CALENDAR: happens once per SkyBlock year starting at a fixed (month, day), for a given
 *    number of SkyBlock days.
 *  - CUSTOM: user-registered, arbitrary real-world period (see EventTracker.trackCustom).
 *
 * Deliberately NOT included: Traveling Zoo (chance-based, not a fixed date), Mining Fiesta /
 * Fishing Festival / Mythological Ritual (mayor-perk dependent or unconfirmed schedule at the
 * time this was written). Verify against the wiki before adding more - a wrong date means a
 * false @everyone ping.
 */
public enum EventType {

    DARK_AUCTION(
            "\ud83d\udd28 Dark Auction",
            Kind.FIXED_PERIOD,
            3L * SkyBlockCalendar.DAY_MS, // every 3 SkyBlock days = every real hour
            -1, -1, -1
    ),

    NEW_YEAR(
            "\ud83c\udf86 New Year Celebration",
            Kind.CALENDAR,
            -1,
            0, 1, // Early Spring 1st
            1
    ),

    SPOOKY_FESTIVAL(
            "\ud83c\udf83 Spooky Festival",
            Kind.CALENDAR,
            -1,
            7, 29, // Autumn 29th
            3
    ),

    SEASON_OF_JERRY(
            "\u2744\ufe0f Jerry's Workshop opens (Late Winter)",
            Kind.CALENDAR,
            -1,
            11, 1, // Late Winter 1st, the whole month
            31
    ),

    HARVEST_FEAST(
            "\ud83c\udf3d Harvest Feast begins",
            Kind.CALENDAR,
            -1,
            6, 1, // Early Autumn 1st, runs the whole autumn season
            93
    ),

    CUSTOM("\ud83d\udcc5 Custom Event", Kind.CUSTOM, -1, -1, -1, -1);

    public enum Kind {
        FIXED_PERIOD, CALENDAR, CUSTOM
    }

    public final String displayName;
    public final Kind kind;
    /** Only meaningful for FIXED_PERIOD. */
    public final long periodMs;
    /** Only meaningful for CALENDAR: 0-11. */
    public final int monthIndex;
    /** Only meaningful for CALENDAR: 1-31. */
    public final int startDay;
    /** Only meaningful for CALENDAR: how many SkyBlock days the event runs for. */
    public final int durationSkyblockDays;

    EventType(String displayName, Kind kind, long periodMs, int monthIndex, int startDay, int durationSkyblockDays) {
        this.displayName = displayName;
        this.kind = kind;
        this.periodMs = periodMs;
        this.monthIndex = monthIndex;
        this.startDay = startDay;
        this.durationSkyblockDays = durationSkyblockDays;
    }
}
