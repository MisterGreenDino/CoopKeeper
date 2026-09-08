package org.bot.utils;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers the last message this bot sent per "tracker key" (e.g. one specific tracked event,
 * or "mayor:&lt;channelId&gt;"). Before posting a fresh update, callers delete the previously
 * tracked message so channels don't fill up with stale pings - only the latest automated message
 * stays visible.
 *
 * We always send a brand-new message rather than editing in place: Discord only re-notifies
 * @mentions on message creation, not on edits, so an edit-in-place approach would silently stop
 * pinging people.
 */
public final class ChannelMessageTracker {

    private static final Map<String, String> lastMessageId = new ConcurrentHashMap<>();

    private ChannelMessageTracker() {
    }

    /** Deletes the previously tracked message for this key in this channel, if any. Fire-and-forget. */
    public static void deletePrevious(TextChannel channel, String key) {
        String messageId = lastMessageId.get(key);
        if (messageId == null) {
            return;
        }
        channel.deleteMessageById(messageId).queue(
                unused -> {
                },
                error -> {
                    // Already deleted, too old to bulk-delete, missing permissions, etc. - nothing to do.
                }
        );
    }

    public static void remember(String key, String messageId) {
        lastMessageId.put(key, messageId);
    }

    public static void forget(String key) {
        lastMessageId.remove(key);
    }

    /** The last known message id for this key, or null if none is tracked (yet, or ever). */
    public static String get(String key) {
        return lastMessageId.get(key);
    }

    /** Snapshot of every tracked message id, for {@link org.bot.persistence.Persistence}. */
    public static java.util.Map<String, String> exportState() {
        return new java.util.HashMap<>(lastMessageId);
    }

    /** Restores previously saved message ids so the next update edits/deletes the right message instead of starting fresh. */
    public static void restore(java.util.Map<String, String> saved) {
        lastMessageId.putAll(saved);
    }
}
