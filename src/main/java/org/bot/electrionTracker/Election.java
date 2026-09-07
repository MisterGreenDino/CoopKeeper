package org.bot.electrionTracker;

import java.util.List;

/** An ongoing (or most recent) mayoral election. */
public class Election {

    public final int year;
    public final List<Candidate> candidates;

    public Election(int year, List<Candidate> candidates) {
        this.year = year;
        this.candidates = candidates;
    }

    /** Candidate currently leading in votes, or null if no votes have been reported yet. */
    public Candidate leader() {
        Candidate best = null;
        for (Candidate c : candidates) {
            if (best == null || c.votes > best.votes) {
                best = c;
            }
        }
        return best;
    }
}
