package org.bot.skillXPCalculator;

/** Result of a dungeon class (or Catacombs) level lookup: exact level + fractional progress. */
public class DungeonClassXP {

    public final DungeonClass dungeonClass; // null when this represents Catacombs itself
    public final double totalXp;
    public final int level;
    public final double fractionalLevel;

    public DungeonClassXP(DungeonClass dungeonClass, double totalXp, int level, double fractionalLevel) {
        this.dungeonClass = dungeonClass;
        this.totalXp = totalXp;
        this.level = level;
        this.fractionalLevel = fractionalLevel;
    }
}
