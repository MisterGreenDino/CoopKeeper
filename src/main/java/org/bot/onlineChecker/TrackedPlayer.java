package org.bot.onlineChecker;

import java.time.Instant;
import java.util.Objects;

/** A player being watched by {@link PlayerTracker}, with cached status from the last poll. */
public class TrackedPlayer {

    public final String username;
    public final String uuid;

    public volatile boolean statusKnown = false;
    public volatile boolean online;
    public volatile String gameType;
    public volatile String mode;
    /** When this player was last seen going offline. Null while online or status not yet known. */
    public volatile Instant offlineSince;

    public TrackedPlayer(String username, String uuid) {
        this.username = username;
        this.uuid = uuid;
    }

    // Equality by uuid so the same account can't be tracked twice under different capitalizations.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TrackedPlayer other)) {
            return false;
        }
        return uuid.equals(other.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
