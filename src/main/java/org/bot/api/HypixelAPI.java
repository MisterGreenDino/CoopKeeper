package org.bot.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bot.utils.Log;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin wrapper around the Hypixel Public API (v2).
 *
 * The API key is read from the HYPIXEL_API_KEY environment variable - never hardcode it here.
 * Get one at https://developer.hypixel.net/dashboard (dev keys expire every 2 days until your
 * application is reviewed, permanent keys after).
 */
public final class HypixelAPI {

    private static final String BASE_URL = "https://api.hypixel.net/v2";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private HypixelAPI() {
    }

    private static String apiKey() {
        String key = System.getenv("HYPIXEL_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("HYPIXEL_API_KEY missing - set it as an environment variable");
        }
        return key;
    }

    private static HypixelResponse request(String path, boolean authenticated) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(10))
                    .GET();

            if (authenticated) {
                builder.header("API-Key", apiKey());
            }

            HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            boolean success = json.has("success") && json.get("success").getAsBoolean();
            String cause = json.has("cause") ? json.get("cause").getAsString() : null;

            if (!success) {
                Log.warn("Hypixel API call failed [" + path + "] (HTTP " + response.statusCode() + "): " + cause);
            }

            return new HypixelResponse(success, cause, json);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            Log.error("Hypixel API request failed [" + path + "]: " + e.getMessage());
            return new HypixelResponse(false, e.getMessage(), null);
        }
    }

    /** GET /player - basic Hypixel network player data. Requires a key. */
    public static HypixelResponse getPlayer(String uuid) {
        return request("/player?uuid=" + uuid, true);
    }

    /** GET /status - whether a player is online and what they're playing. Requires a key. */
    public static HypixelResponse getStatus(String uuid) {
        return request("/status?uuid=" + uuid, true);
    }

    /** GET /skyblock/profiles - all SkyBlock profiles (islands) for a player. Requires a key. */
    public static HypixelResponse getSkyblockProfiles(String uuid) {
        return request("/skyblock/profiles?uuid=" + uuid, true);
    }

    /** GET /skyblock/profile - a single SkyBlock profile by its profile id. Requires a key. */
    public static HypixelResponse getSkyblockProfile(String profileId) {
        return request("/skyblock/profile?profile=" + profileId, true);
    }

    /** GET /resources/skyblock/election - current mayor + election candidates. Public, no key needed. */
    public static HypixelResponse getSkyblockElection() {
        return request("/resources/skyblock/election", false);
    }

    /** GET /resources/skyblock/bingo - the current Bingo event and its goals. Public, no key needed. */
    public static HypixelResponse getSkyblockBingo() {
        return request("/resources/skyblock/bingo", false);
    }

    /** GET /resources/skyblock/skills - skill XP-per-level tables. Public, no key needed. */
    public static HypixelResponse getSkyblockSkills() {
        return request("/resources/skyblock/skills", false);
    }
}
