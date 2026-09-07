package org.bot.profileChecker;

/** Lightweight metadata about one of a player's SkyBlock profiles (islands). */
public class SkyBlockProfile {

    public final String profileId;
    public final String cuteName;
    public final boolean selected;
    public final String gameMode;

    public SkyBlockProfile(String profileId, String cuteName, boolean selected, String gameMode) {
        this.profileId = profileId;
        this.cuteName = cuteName;
        this.selected = selected;
        this.gameMode = gameMode;
    }

    @Override
    public String toString() {
        return cuteName + (gameMode != null ? " (" + gameMode + ")" : "");
    }
}
