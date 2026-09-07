package org.bot.profileChecker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bot.api.HypixelAPI;
import org.bot.api.HypixelResponse;
import org.bot.skillXPCalculator.SkillType;
import org.bot.skillXPCalculator.SkillXPCalculator;
import org.bot.utils.Log;
import org.bot.utils.ProfileGrabber;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches and parses a player's SkyBlock profiles from the Hypixel API.
 *
 * NOTE ON THE API SHAPE: the SkyBlock member object is large and has changed shape a few
 * times over the years (currently: currencies.coin_purse, player_data.experience.SKILL_*,
 * banking.balance on the profile root rather than per-member). If fields come back as 0/empty
 * unexpectedly, print the raw JSON from a live response and adjust the paths below - this
 * sandbox can't reach api.hypixel.net directly to verify against a live payload.
 */
public final class ProfileChecker {

    private ProfileChecker() {
    }

    public static PlayerProfile getByUsername(String username) {
        String uuid = ProfileGrabber.getUuid(username);
        if (uuid == null) {
            throw new IllegalArgumentException("Unknown Minecraft account: " + username);
        }
        return getByUuid(uuid, username);
    }

    public static PlayerProfile getByUuid(String uuid) {
        return getByUuid(uuid, uuid);
    }

    private static PlayerProfile getByUuid(String uuid, String displayName) {
        PlayerProfile cached = ProfileCache.get(uuid);
        if (cached != null) {
            return cached;
        }

        HypixelResponse response = HypixelAPI.getSkyblockProfiles(uuid);
        if (!response.isSuccess() || response.getData() == null) {
            throw new IllegalStateException("Hypixel API error: " + response.getCause());
        }

        JsonElement profilesElement = response.getData().get("profiles");
        if (profilesElement == null || profilesElement.isJsonNull()) {
            throw new IllegalStateException("That player has no SkyBlock profiles");
        }

        List<SkyBlockProfile> profiles = new ArrayList<>();
        SkyBlockProfile selectedMeta = null;
        JsonObject selectedProfileRoot = null;
        JsonObject selectedMember = null;

        JsonArray profilesArray = profilesElement.getAsJsonArray();
        for (JsonElement el : profilesArray) {
            JsonObject profileJson = el.getAsJsonObject();
            String profileId = profileJson.get("profile_id").getAsString();
            String cuteName = profileJson.has("cute_name") ? profileJson.get("cute_name").getAsString() : profileId;
            boolean selected = profileJson.has("selected") && profileJson.get("selected").getAsBoolean();
            String gameMode = profileJson.has("game_mode") ? profileJson.get("game_mode").getAsString() : null;

            SkyBlockProfile meta = new SkyBlockProfile(profileId, cuteName, selected, gameMode);
            profiles.add(meta);

            if (selected) {
                selectedMeta = meta;
                selectedProfileRoot = profileJson;
                JsonObject members = profileJson.getAsJsonObject("members");
                if (members != null && members.has(uuid)) {
                    selectedMember = members.getAsJsonObject(uuid);
                }
            }
        }

        // Fall back to the first profile if the API didn't flag one as selected.
        if (selectedMeta == null && !profiles.isEmpty()) {
            JsonObject profileJson = profilesArray.get(0).getAsJsonObject();
            selectedMeta = profiles.get(0);
            selectedProfileRoot = profileJson;
            JsonObject members = profileJson.getAsJsonObject("members");
            if (members != null && members.has(uuid)) {
                selectedMember = members.getAsJsonObject(uuid);
            }
        }

        double purse = 0;
        double bank = 0;
        double skillAverage = 0;
        Map<SkillType, Double> skillLevels = new EnumMap<>(SkillType.class);

        if (selectedMember != null) {
            JsonObject currencies = selectedMember.getAsJsonObject("currencies");
            if (currencies != null && currencies.has("coin_purse")) {
                purse = currencies.get("coin_purse").getAsDouble();
            }
            skillLevels = computeSkillLevels(selectedMember);
            skillAverage = averageExcluding(skillLevels, SkillType.RUNECRAFTING, SkillType.SOCIAL);
        }
        if (selectedProfileRoot != null) {
            JsonObject banking = selectedProfileRoot.getAsJsonObject("banking");
            if (banking != null && banking.has("balance")) {
                bank = banking.get("balance").getAsDouble();
            }
        }

        PlayerProfile result = new PlayerProfile(uuid, displayName, profiles, selectedMeta, purse, bank, skillAverage, skillLevels);
        ProfileCache.put(uuid, result);
        return result;
    }

    private static Map<SkillType, Double> computeSkillLevels(JsonObject member) {
        Map<SkillType, Double> levels = new EnumMap<>(SkillType.class);
        JsonObject playerData = member.getAsJsonObject("player_data");
        JsonObject experience = playerData != null ? playerData.getAsJsonObject("experience") : null;
        if (experience == null) {
            Log.warn("No skill experience found - is the player's Skills API setting enabled?");
            return levels;
        }

        for (SkillType type : SkillType.values()) {
            if (experience.has(type.apiKey)) {
                double xp = experience.get(type.apiKey).getAsDouble();
                levels.put(type, SkillXPCalculator.getLevel(type, xp));
            }
        }
        return levels;
    }

    private static double averageExcluding(Map<SkillType, Double> levels, SkillType... excluded) {
        List<SkillType> skip = List.of(excluded);
        double sum = 0;
        int count = 0;
        for (Map.Entry<SkillType, Double> entry : levels.entrySet()) {
            if (skip.contains(entry.getKey())) {
                continue;
            }
            sum += entry.getValue();
            count++;
        }
        return count == 0 ? 0 : sum / count;
    }
}
