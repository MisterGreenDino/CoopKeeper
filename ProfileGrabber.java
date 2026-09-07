package org.bot.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Map;

/**
 * Resolves Minecraft usernames to UUIDs via the Mojang API, with a small in-memory cache
 * (usernames can be recycled, but not within the lifetime of a bot process).
 */
public final class ProfileGrabber {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(30);
    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private record CacheEntry(String uuid, long expiresAt) {
    }

    private ProfileGrabber() {
    }

    /** Resolves a Minecraft username (case-insensitive) to its dashed UUID, or null if the account doesn't exist. */
    public static String getUuid(String username) {
        String key = username.toLowerCase();
        CacheEntry cached = CACHE.get(key);
        if (cached != null && System.currentTimeMillis() < cached.expiresAt()) {
            return cached.uuid();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + username))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 204 || response.statusCode() == 404) {
                return null;
            }
            if (response.statusCode() != 200) {
                Log.warn("Mojang lookup failed for " + username + ": HTTP " + response.statusCode());
                return null;
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String dashed = dash(json.get("id").getAsString());
            CACHE.put(key, new CacheEntry(dashed, System.currentTimeMillis() + CACHE_TTL_MS));
            return dashed;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            Log.error("UUID lookup failed for " + username + ": " + e.getMessage());
            return null;
        }
    }

    private static String dash(String raw) {
        return raw.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"
        );
    }
}
