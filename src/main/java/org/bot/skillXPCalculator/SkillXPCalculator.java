package org.bot.skillXPCalculator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bot.api.HypixelAPI;
import org.bot.api.HypixelResponse;
import org.bot.utils.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts raw skill XP into fractional skill levels, using the level tables published at
 * GET /resources/skyblock/skills (public, no API key needed).
 *
 * The exact JSON field names below (levels[].totalExpRequired) are based on how every major
 * community SkyBlock library parses this resource. This sandbox can't reach api.hypixel.net to
 * verify a live payload, so if levels come out wrong, log response.getData() once and check the
 * field names against what's actually returned.
 */
public final class SkillXPCalculator {

    private static volatile boolean loaded = false;

    private SkillXPCalculator() {
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }

        HypixelResponse response = HypixelAPI.getSkyblockSkills();
        if (!response.isSuccess() || response.getData() == null) {
            Log.error("Failed to load skill XP tables: " + response.getCause());
            return;
        }

        JsonObject skills = response.getData().getAsJsonObject("skills");
        if (skills == null) {
            Log.error("resources/skyblock/skills response had no 'skills' object");
            return;
        }

        for (SkillType type : SkillType.values()) {
            if (!skills.has(type.apiKey)) {
                continue;
            }
            JsonObject skillJson = skills.getAsJsonObject(type.apiKey);
            JsonArray levels = skillJson.getAsJsonArray("levels");
            if (levels == null) {
                continue;
            }

            List<Double> cumulative = new ArrayList<>();
            double running = 0;
            for (int i = 0; i < levels.size(); i++) {
                JsonObject lvl = levels.get(i).getAsJsonObject();
                double required = lvl.has("totalExpRequired")
                        ? lvl.get("totalExpRequired").getAsDouble()
                        : lvl.has("exp") ? lvl.get("exp").getAsDouble() : 0;
                running += required;
                cumulative.add(running);
            }

            double[] table = new double[cumulative.size()];
            for (int i = 0; i < table.length; i++) {
                table[i] = cumulative.get(i);
            }
            Skill.LEVEL_TABLES.put(type, table);
        }

        loaded = true;
    }

    /** Forces the level tables to be re-fetched next time they're needed (e.g. after a game update). */
    public static synchronized void reload() {
        loaded = false;
        Skill.LEVEL_TABLES.clear();
    }

    /** Fractional level (e.g. 42.37) for the given total skill XP. Returns 0 if the table isn't available. */
    public static double getLevel(SkillType type, double totalXp) {
        ensureLoaded();
        double[] table = Skill.LEVEL_TABLES.get(type);
        if (table == null || table.length == 0) {
            return 0;
        }

        int level = 0;
        for (int i = 0; i < table.length; i++) {
            if (totalXp >= table[i]) {
                level = i + 1;
            } else {
                break;
            }
        }

        if (level >= table.length) {
            return level;
        }

        double lower = level == 0 ? 0 : table[level - 1];
        double upper = table[level];
        double progress = upper > lower ? (totalXp - lower) / (upper - lower) : 0;
        return level + Math.max(0, Math.min(1, progress));
    }

    public static int getExactLevel(SkillType type, double totalXp) {
        return (int) getLevel(type, totalXp);
    }
}
