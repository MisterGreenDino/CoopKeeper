package org.bot.profileChecker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Short-lived cache of {@link PlayerProfile} lookups, keyed by player UUID.
 * Keeps us from hammering the Hypixel API when the same player is looked up repeatedly
 * (e.g. multiple slash commands in quick succession).
 */
public final class ProfileCache {

    private static final long TTL_MS = TimeUnit.SECONDS.toMillis(60);
    private static final Map<String, Entry> cache = new ConcurrentHashMap<>();

    private record Entry(PlayerProfile profile, long expiresAt) {
    }

    private ProfileCache() {
    }

    public static PlayerProfile get(String uuid) {
        Entry entry = cache.get(uuid);
        if (entry == null || System.currentTimeMillis() > entry.expiresAt()) {
            return null;
        }
        return entry.profile();
    }

    public static void put(String uuid, PlayerProfile profile) {
        cache.put(uuid, new Entry(profile, System.currentTimeMillis() + TTL_MS));
    }

    public static void invalidate(String uuid) {
        cache.remove(uuid);
    }
}
