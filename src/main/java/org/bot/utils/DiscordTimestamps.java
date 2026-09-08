package org.bot.utils;

import java.time.Instant;

/**
 * Formats Discord's native <t:UNIX:STYLE> timestamp tags. These render client-side, auto-update
 * live, and show in each viewer's own timezone - much better than a manually formatted "in Xh Ym"
 * string that goes stale the moment the message is sent.
 *
 * Styles: t=short time, T=long time, d=short date, D=long date, f=short date+time,
 * F=long date+time (with weekday), R=relative ("in 3 hours").
 */
public final class DiscordTimestamps {

    private DiscordTimestamps() {
    }

    public static String format(Instant instant, char style) {
        return "<t:" + instant.getEpochSecond() + ":" + style + ">";
    }

    /** e.g. "in 3 hours" / "2 days ago" - auto-updates in the Discord client. */
    public static String relative(Instant instant) {
        return format(instant, 'R');
    }

    /** e.g. "September 8, 2026 3:55 PM". */
    public static String full(Instant instant) {
        return format(instant, 'F');
    }

    /** e.g. "September 8, 2026". */
    public static String shortDate(Instant instant) {
        return format(instant, 'd');
    }

    /** Relative + short time, e.g. "in 3 hours (3:55 PM)". */
    public static String relativeAndTime(Instant instant) {
        return relative(instant) + " (" + format(instant, 't') + ")";
    }
}
