package org.bot.skillXPCalculator;

import com.google.gson.JsonObject;

/**
 * Converts Catacombs / dungeon-class XP into levels.
 *
 * Unlike normal skills, the dungeon leveling curve is NOT published by the Hypixel resources
 * API - it's community-maintained (mirrored here from the widely-used NotEnoughUpdates data).
 * Double check these numbers against a current NEU/SkyHelper repo before relying on them for
 * anything precise, they can drift when Hypixel adds levels above 50.
 */
public final class DungeonXP {

    // Index i = XP required to go from level i to level i+1 (i.e. levels[0] is XP for level 1).
    private static final double[] XP_PER_LEVEL = {
            50, 75, 110, 160, 230, 330, 470, 670, 950, 1340,
            1890, 2665, 3760, 5260, 7380, 10300, 14400, 20000, 27600, 38000,
            52500, 71500, 97000, 132000, 180000, 243000, 328000, 445000, 600000, 800000,
            1065000, 1410000, 1900000, 2500000, 3300000, 4300000, 5600000, 7200000, 9200000, 12000000,
            15000000, 19000000, 24000000, 30000000, 38000000, 48000000, 60000000, 75000000, 93000000, 116250000
    };

    private static final double[] CUMULATIVE = buildCumulative();

    private DungeonXP() {
    }

    private static double[] buildCumulative() {
        double[] cumulative = new double[XP_PER_LEVEL.length];
        double running = 0;
        for (int i = 0; i < XP_PER_LEVEL.length; i++) {
            running += XP_PER_LEVEL[i];
            cumulative[i] = running;
        }
        return cumulative;
    }

    public static int maxLevel() {
        return XP_PER_LEVEL.length;
    }

    /** Fractional level (e.g. 24.6) for the given total XP, capped display-wise at {@link #maxLevel()}. */
    public static double getFractionalLevel(double totalXp) {
        int level = 0;
        for (int i = 0; i < CUMULATIVE.length; i++) {
            if (totalXp >= CUMULATIVE[i]) {
                level = i + 1;
            } else {
                break;
            }
        }

        if (level >= CUMULATIVE.length) {
            return level;
        }

        double lower = level == 0 ? 0 : CUMULATIVE[level - 1];
        double upper = CUMULATIVE[level];
        double progress = (totalXp - lower) / (upper - lower);
        return level + Math.max(0, Math.min(1, progress));
    }

    public static int getLevel(double totalXp) {
        return (int) getFractionalLevel(totalXp);
    }

    /** Parses Catacombs level from member.player_data.dungeons.dungeon_types.catacombs.experience. */
    public static DungeonClassXP catacombsFrom(JsonObject member) {
        double xp = 0;
        JsonObject dungeons = member.getAsJsonObject("player_data") != null
                ? member.getAsJsonObject("player_data").getAsJsonObject("dungeons")
                : member.getAsJsonObject("dungeons"); // some API snapshots keep it at member root
        if (dungeons != null) {
            JsonObject dungeonTypes = dungeons.getAsJsonObject("dungeon_types");
            JsonObject catacombs = dungeonTypes != null ? dungeonTypes.getAsJsonObject("catacombs") : null;
            if (catacombs != null && catacombs.has("experience")) {
                xp = catacombs.get("experience").getAsDouble();
            }
        }
        double fractional = getFractionalLevel(xp);
        return new DungeonClassXP(null, xp, (int) fractional, fractional);
    }

    /** Parses a single class's level from member.player_data.dungeons.player_classes.<class>.experience. */
    public static DungeonClassXP classFrom(JsonObject member, DungeonClass dungeonClass) {
        double xp = 0;
        JsonObject dungeons = member.getAsJsonObject("player_data") != null
                ? member.getAsJsonObject("player_data").getAsJsonObject("dungeons")
                : member.getAsJsonObject("dungeons");
        if (dungeons != null) {
            JsonObject classes = dungeons.getAsJsonObject("player_classes");
            JsonObject classJson = classes != null ? classes.getAsJsonObject(dungeonClass.apiKey) : null;
            if (classJson != null && classJson.has("experience")) {
                xp = classJson.get("experience").getAsDouble();
            }
        }
        double fractional = getFractionalLevel(xp);
        return new DungeonClassXP(dungeonClass, xp, (int) fractional, fractional);
    }
}
