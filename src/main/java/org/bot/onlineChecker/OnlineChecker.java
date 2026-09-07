package org.bot.onlineChecker;

import com.google.gson.JsonObject;
import org.bot.api.HypixelAPI;
import org.bot.api.HypixelResponse;
import org.bot.utils.ProfileGrabber;

public final class OnlineChecker {

    private OnlineChecker() {
    }

    public static OnlineStatus checkByUsername(String username) {
        String uuid = ProfileGrabber.getUuid(username);
        if (uuid == null) {
            throw new IllegalArgumentException("Unknown Minecraft account: " + username);
        }
        return checkByUuid(uuid);
    }

    public static OnlineStatus checkByUuid(String uuid) {
        HypixelResponse response = HypixelAPI.getStatus(uuid);
        if (!response.isSuccess() || response.getData() == null) {
            throw new IllegalStateException("Hypixel API error: " + response.getCause());
        }

        JsonObject session = response.getData().getAsJsonObject("session");
        if (session == null) {
            return OnlineStatus.offline();
        }

        boolean online = session.has("online") && session.get("online").getAsBoolean();
        if (!online) {
            return OnlineStatus.offline();
        }

        String gameType = session.has("gameType") ? session.get("gameType").getAsString() : null;
        String mode = session.has("mode") ? session.get("mode").getAsString() : null;
        String map = session.has("map") ? session.get("map").getAsString() : null;
        return new OnlineStatus(true, gameType, mode, map);
    }
}
