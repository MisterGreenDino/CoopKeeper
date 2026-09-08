package org.bot.electrionTracker;

/** A single mayor/candidate perk, as published by /resources/skyblock/election. */
public class Perk {

    public final String name;
    public final String description;

    public Perk(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
