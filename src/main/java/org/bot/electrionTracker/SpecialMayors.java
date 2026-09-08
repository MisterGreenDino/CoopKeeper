package org.bot.electrionTracker;

import org.bot.eventTracker.SkyBlockCalendar;

import java.time.Instant;
import java.util.List;

/**
 * Special Candidates (Derpy, Jerry, Scorpius) don't run by random chance like regular candidates -
 * they join the election in a fixed rotating sequence, one every 8 SkyBlock years, so each
 * individual special candidate resurfaces every 24 SkyBlock years. Confirmed on the SkyBlock Wiki
 * "Mayor Election" page, "Candidate Selection" section, including this exact schedule table:
 *   Derpy: Year 512 -> 536      Jerry: Year 496 -> 520      Scorpius: Year 504 -> 528
 * "Year X" here means Election Year X (candidates revealed Late Summer 27th of SkyBlock Year X,
 * winner takes office the following Late Spring 27th).
 *
 * NOTE: Dante (Year 128) and Aura (Year 464) both broke this rotation as one-time exceptions -
 * if that happens again, this schedule will be off until manually corrected.
 */
public final class SpecialMayors {

    public record SpecialMayor(String name, long lastKnownParticipationYear) {
    }

    public static final List<SpecialMayor> ALL = List.of(
            new SpecialMayor("Derpy", 512),
            new SpecialMayor("Candidate Jerry", 496),
            new SpecialMayor("Scorpius", 504)
    );

    private static final int PARTICIPATION_MONTH = 5; // Late Summer
    private static final int PARTICIPATION_DAY = 27;

    private SpecialMayors() {
    }

    /** Next real-world instant this special candidate is revealed as a candidate again (Late Summer 27th of their next Election Year). */
    public static Instant nextAppearance(SpecialMayor mayor, Instant now) {
        long year = mayor.lastKnownParticipationYear();
        Instant candidate = SkyBlockCalendar.instantOf(year, PARTICIPATION_MONTH, PARTICIPATION_DAY);
        while (!candidate.isAfter(now)) {
            year += 24;
            candidate = SkyBlockCalendar.instantOf(year, PARTICIPATION_MONTH, PARTICIPATION_DAY);
        }
        return candidate;
    }
}
