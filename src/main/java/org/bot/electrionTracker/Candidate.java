package org.bot.electrionTracker;

import java.util.List;

/** One candidate running in a SkyBlock mayoral election. */
public class Candidate {

    public final String key;
    public final String name;
    public final List<Perk> perks;
    public final int votes;

    public Candidate(String key, String name, List<Perk> perks, int votes) {
        this.key = key;
        this.name = name;
        this.perks = perks;
        this.votes = votes;
    }
}
