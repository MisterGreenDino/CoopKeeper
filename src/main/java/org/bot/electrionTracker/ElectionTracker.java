package org.bot.electrionTracker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bot.api.HypixelAPI;
import org.bot.api.HypixelResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Fetches the current SkyBlock mayor and any ongoing election from
 * GET /resources/skyblock/election (public, no API key needed).
 *
 * Cached briefly since this resource only changes a few times a day.
 */
public final class ElectionTracker {

    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(5);

    private static volatile Mayor cachedMayor;
    private static volatile Election cachedElection;
    private static volatile long cacheExpiresAt;

    private ElectionTracker() {
    }

    private static synchronized void refreshIfStale() {
        if (System.currentTimeMillis() < cacheExpiresAt) {
            return;
        }

        HypixelResponse response = HypixelAPI.getSkyblockElection();
        if (!response.isSuccess() || response.getData() == null) {
            throw new IllegalStateException("Hypixel API error: " + response.getCause());
        }

        JsonObject data = response.getData();

        JsonObject mayorJson = data.getAsJsonObject("mayor");
        cachedMayor = mayorJson != null ? parseMayor(mayorJson) : null;

        JsonObject currentJson = data.getAsJsonObject("current");
        cachedElection = currentJson != null ? parseElection(currentJson) : null;

        cacheExpiresAt = System.currentTimeMillis() + CACHE_TTL_MS;
    }

    private static Mayor parseMayor(JsonObject mayorJson) {
        String key = mayorJson.has("key") ? mayorJson.get("key").getAsString() : null;
        String name = mayorJson.has("name") ? mayorJson.get("name").getAsString() : "Unknown";
        List<Perk> perks = parsePerks(mayorJson.getAsJsonArray("perks"));

        String minister = null;
        JsonObject ministerJson = mayorJson.getAsJsonObject("minister");
        if (ministerJson != null && ministerJson.has("name")) {
            minister = ministerJson.get("name").getAsString();
        }

        return new Mayor(key, name, perks, minister);
    }

    private static Election parseElection(JsonObject electionJson) {
        int year = electionJson.has("year") ? electionJson.get("year").getAsInt() : 0;
        List<Candidate> candidates = new ArrayList<>();

        JsonArray candidatesArray = electionJson.getAsJsonArray("candidates");
        if (candidatesArray != null) {
            for (JsonElement el : candidatesArray) {
                JsonObject c = el.getAsJsonObject();
                String key = c.has("key") ? c.get("key").getAsString() : null;
                String name = c.has("name") ? c.get("name").getAsString() : "Unknown";
                int votes = c.has("votes") ? c.get("votes").getAsInt() : 0;
                List<Perk> perks = parsePerks(c.getAsJsonArray("perks"));
                candidates.add(new Candidate(key, name, perks, votes));
            }
        }

        return new Election(year, candidates);
    }

    private static List<Perk> parsePerks(JsonArray perksArray) {
        List<Perk> perks = new ArrayList<>();
        if (perksArray == null) {
            return perks;
        }
        for (JsonElement el : perksArray) {
            JsonObject perk = el.getAsJsonObject();
            String name = perk.has("name") ? perk.get("name").getAsString() : "Unknown perk";
            String description = perk.has("description") ? perk.get("description").getAsString() : "";
            perks.add(new Perk(name, description));
        }
        return perks;
    }

    /** The current mayor, or null if the resource didn't include one. */
    public static Mayor getCurrentMayor() {
        refreshIfStale();
        return cachedMayor;
    }

    /** The in-progress election, or null if there isn't one running right now. */
    public static Election getOngoingElection() {
        refreshIfStale();
        return cachedElection;
    }
}
