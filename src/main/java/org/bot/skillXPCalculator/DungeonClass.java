package org.bot.skillXPCalculator;

/** A Catacombs dungeon class. {@code apiKey} matches player_data.dungeons.player_classes in the API. */
public enum DungeonClass {
    HEALER("healer"),
    MAGE("mage"),
    BERSERK("berserk"),
    ARCHER("archer"),
    TANK("tank");

    public final String apiKey;

    DungeonClass(String apiKey) {
        this.apiKey = apiKey;
    }
}
