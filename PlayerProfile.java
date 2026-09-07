package org.bot.profileChecker;

import org.bot.skillXPCalculator.SkillType;

import java.util.List;
import java.util.Map;

/** Aggregated, human-friendly view of a player's SkyBlock data on their selected profile. */
public class PlayerProfile {

    public final String uuid;
    public final String username;
    public final List<SkyBlockProfile> profiles;
    public final SkyBlockProfile selected;
    public final double purse;
    public final double bank;
    public final double skillAverage;
    /** Fractional level per skill (e.g. FARMING -> 34.2). Empty if the player's Skills API is off. */
    public final Map<SkillType, Double> skillLevels;

    public PlayerProfile(String uuid, String username, List<SkyBlockProfile> profiles,
                          SkyBlockProfile selected, double purse, double bank, double skillAverage,
                          Map<SkillType, Double> skillLevels) {
        this.uuid = uuid;
        this.username = username;
        this.profiles = profiles;
        this.selected = selected;
        this.purse = purse;
        this.bank = bank;
        this.skillAverage = skillAverage;
        this.skillLevels = skillLevels;
    }
}
