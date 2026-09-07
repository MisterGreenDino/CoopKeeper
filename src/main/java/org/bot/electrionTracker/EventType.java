package org.bot.eventTracker;

/**
 * Known recurring SkyBlock events with a fixed real-world period.
 * DARK_AUCTION is the only one with a precisely documented schedule (every real-life hour,
 * on the hour, running for a few minutes) - it doesn't depend on in-game SkyBlock date math.
 * CUSTOM covers anything registered manually with an arbitrary interval (e.g. via /event_add).
 */
public enum EventType {
    DARK_AUCTION("Dark Auction", 60L * 60L * 1000L),
    CUSTOM("Custom Event", -1);

    public final String displayName;
    /** Fixed period in milliseconds, or -1 if the interval is set per-event (CUSTOM). */
    public final long periodMs;

    EventType(String displayName, long periodMs) {
        this.displayName = displayName;
        this.periodMs = periodMs;
    }
}
