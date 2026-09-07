package org.bot.electrionTracker;

import java.util.List;

/** One candidate running in a SkyBlock mayoral election. */
public class Candidate {

    public final String key;
    public final String name;
    public final List<String> perks;
    public final int votes;

    public Candidate(String key, String name, List<String> perks, int votes) {
        this.key = key;
        this.name = name;
        this.perks = perks;
        this.votes = votes;
    }
}
