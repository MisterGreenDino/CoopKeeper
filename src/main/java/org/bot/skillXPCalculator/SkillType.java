package org.bot.skillXPCalculator;

/**
 * A SkyBlock skill. {@code apiKey} matches the key used under
 * {@code member.player_data.experience} in the Hypixel API, and under
 * {@code resources/skyblock/skills} for the level-up tables.
 */
public enum SkillType {
    FARMING("SKILL_FARMING"),
    MINING("SKILL_MINING"),
    COMBAT("SKILL_COMBAT"),
    FORAGING("SKILL_FORAGING"),
    FISHING("SKILL_FISHING"),
    ENCHANTING("SKILL_ENCHANTING"),
    ALCHEMY("SKILL_ALCHEMY"),
    TAMING("SKILL_TAMING"),
    CARPENTRY("SKILL_CARPENTRY"),
    RUNECRAFTING("SKILL_RUNECRAFTING"),
    SOCIAL("SKILL_SOCIAL");

    public final String apiKey;

    SkillType(String apiKey) {
        this.apiKey = apiKey;
    }

    public static SkillType fromApiKey(String apiKey) {
        for (SkillType type : values()) {
            if (type.apiKey.equalsIgnoreCase(apiKey)) {
                return type;
            }
        }
        return null;
    }
}
