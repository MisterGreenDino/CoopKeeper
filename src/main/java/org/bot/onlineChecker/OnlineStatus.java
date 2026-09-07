package org.bot.onlineChecker;

public class OnlineStatus {

    public final boolean online;
    public final String gameType;
    public final String mode;
    public final String map;

    public OnlineStatus(boolean online, String gameType, String mode, String map) {
        this.online = online;
        this.gameType = gameType;
        this.mode = mode;
        this.map = map;
    }

    public static OnlineStatus offline() {
        return new OnlineStatus(false, null, null, null);
    }

    @Override
    public String toString() {
        if (!online) {
            return "Offline";
        }
        StringBuilder sb = new StringBuilder("Online");
        if (gameType != null) {
            sb.append(" - ").append(gameType);
        }
        if (mode != null) {
            sb.append(" (").append(mode).append(")");
        }
        if (map != null) {
            sb.append(" on ").append(map);
        }
        return sb.toString();
    }
}
