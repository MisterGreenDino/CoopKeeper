package org.bot.electrionTracker;

import java.util.List;

/** The currently elected SkyBlock mayor and their active perks. */
public class Mayor {

    public final String key;
    public final String name;
    public final List<String> perks;
    public final String minister; // name of the appointed minister, may be null

    public Mayor(String key, String name, List<String> perks, String minister) {
        this.key = key;
        this.name = name;
        this.perks = perks;
        this.minister = minister;
    }
}
