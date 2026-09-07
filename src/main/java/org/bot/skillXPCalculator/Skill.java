package org.bot.skillXPCalculator;

import java.util.EnumMap;
import java.util.Map;

/**
 * Holds the cumulative XP-per-level tables used by {@link SkillXPCalculator}, populated at
 * runtime from the Hypixel {@code resources/skyblock/skills} endpoint. Index {@code i} in a
 * table holds the total XP needed to reach level {@code i + 1}.
 */
public final class Skill {

    static final Map<SkillType, double[]> LEVEL_TABLES = new EnumMap<>(SkillType.class);

    private Skill() {
    }
}
