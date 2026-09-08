package org.bot.onlineChecker;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bot.Bot;
import org.bot.utils.ChannelMessageTracker;
import org.bot.utils.DiscordTimestamps;
import org.bot.utils.Log;
import org.bot.utils.ProfileGrabber;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tracks a roster of SkyBlock players per channel and keeps ONE auto-updating status message
 * (one Discord embed per player - showing their head, online/offline, current server, and how
 * long they've been offline - up to Discord's 10-embeds-per-message limit).
 *
 * This message is EDITED in place rather than deleted+resent: it's a live dashboard, not a ping
 * alert, so there's no @mention notification to preserve.
 *
 * IMPORTANT: every send/edit that decides the message id runs through the single-threaded
 * {@code scheduler} using BLOCKING (.complete()) calls, not .queue(). That's deliberate - if two
 * updates for the same channel ran concurrently with async .queue() callbacks, both could read
 * "no message yet" before either callback stored the new id, and both would post a brand new
 * message (a real bug this project hit). Running everything on one thread with blocking calls
 * makes "read the last known id, then act" atomic.
 */
public final class PlayerTracker {

    private static final int MAX_PLAYERS_PER_MESSAGE = 10; // Discord's embeds-per-message limit
    private static final long POLL_INTERVAL_SECONDS = 60L;

    private static final Map<String, Set<TrackedPlayer>> byChannel = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private PlayerTracker() {
    }

    static {
        scheduler.scheduleAtFixedRate(PlayerTracker::tick, POLL_INTERVAL_SECONDS, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private static String trackerKey(String channelId) {
        return "player_tracker:" + channelId;
    }

    /** Resolves the username to a UUID and adds them to this channel's tracked roster (idempotent). */
    public static TrackedPlayer add(String channelId, String username) {
        String uuid = ProfileGrabber.getUuid(username);
        if (uuid == null) {
            throw new IllegalArgumentException("Unknown Minecraft account: " + username);
        }
        TrackedPlayer player = new TrackedPlayer(username, uuid);
        Set<TrackedPlayer> set = byChannel.computeIfAbsent(channelId, k -> ConcurrentHashMap.newKeySet());
        set.add(player); // no-op if this uuid is already tracked here
        refreshNow(channelId);
        return player;
    }

    /** Removes a tracked player by username (case-insensitive). Returns true if one was removed. */
    public static boolean remove(String channelId, String username) {
        Set<TrackedPlayer> set = byChannel.get(channelId);
        if (set == null) {
            return false;
        }
        boolean removed = set.removeIf(p -> p.username.equalsIgnoreCase(username));
        if (removed) {
            refreshNow(channelId);
        }
        return removed;
    }

    /** Stops tracking every player in a channel and removes the status board message. */
    public static int clear(String channelId) {
        Set<TrackedPlayer> removed = byChannel.remove(channelId);
        int count = removed == null ? 0 : removed.size();
        if (count > 0) {
            scheduler.execute(() -> {
                if (Bot.jda == null) {
                    return;
                }
                TextChannel channel = Bot.jda.getTextChannelById(channelId);
                if (channel != null) {
                    ChannelMessageTracker.deletePrevious(channel, trackerKey(channelId));
                }
                ChannelMessageTracker.forget(trackerKey(channelId));
            });
        }
        return count;
    }

    public static Set<TrackedPlayer> getTracked(String channelId) {
        return byChannel.getOrDefault(channelId, Set.of());
    }

    /** Forces an immediate refresh of every tracked channel's status message. */
    public static void refreshAllNow() {
        for (String channelId : byChannel.keySet()) {
            scheduler.execute(() -> updateChannel(channelId));
        }
    }

    /**
     * Deletes any stale status message left over from before a restart, then posts a brand new
     * one - rather than trying to edit a message that may be showing minutes/hours-old data.
     * Called once at startup after restoring saved state.
     */
    public static void hardResetAllOnStartup() {
        for (String channelId : byChannel.keySet()) {
            scheduler.execute(() -> {
                if (Bot.jda == null) {
                    return;
                }
                TextChannel channel = Bot.jda.getTextChannelById(channelId);
                if (channel != null) {
                    ChannelMessageTracker.deletePrevious(channel, trackerKey(channelId));
                }
                ChannelMessageTracker.forget(trackerKey(channelId));
                updateChannel(channelId);
            });
        }
    }

    /** Snapshot of everyone tracked in every channel, for {@link org.bot.persistence.Persistence}. */
    public static Map<String, List<org.bot.persistence.BotState.TrackedPlayerData>> exportState() {
        Map<String, List<org.bot.persistence.BotState.TrackedPlayerData>> result = new HashMap<>();
        for (Map.Entry<String, Set<TrackedPlayer>> entry : byChannel.entrySet()) {
            List<org.bot.persistence.BotState.TrackedPlayerData> list = new ArrayList<>();
            for (TrackedPlayer p : entry.getValue()) {
                list.add(new org.bot.persistence.BotState.TrackedPlayerData(p.username, p.uuid));
            }
            result.put(entry.getKey(), list);
        }
        return result;
    }

    /** Re-registers previously saved players using their already-resolved uuid (no Mojang lookup needed). */
    public static void restore(Map<String, List<org.bot.persistence.BotState.TrackedPlayerData>> saved) {
        for (Map.Entry<String, List<org.bot.persistence.BotState.TrackedPlayerData>> entry : saved.entrySet()) {
            Set<TrackedPlayer> set = byChannel.computeIfAbsent(entry.getKey(), k -> ConcurrentHashMap.newKeySet());
            for (org.bot.persistence.BotState.TrackedPlayerData p : entry.getValue()) {
                set.add(new TrackedPlayer(p.username(), p.uuid()));
            }
        }
    }

    /** Polls every tracked player in this channel and rebuilds the status message immediately. */
    private static void refreshNow(String channelId) {
        scheduler.execute(() -> updateChannel(channelId));
    }

    private static void tick() {
        for (String channelId : byChannel.keySet()) {
            try {
                updateChannel(channelId);
            } catch (Exception e) {
                Log.error("Player tracker tick failed for channel " + channelId + ": " + e.getMessage());
            }
        }
    }

    private static void updateChannel(String channelId) {
        Set<TrackedPlayer> players = byChannel.get(channelId);
        if (players == null || players.isEmpty() || Bot.jda == null) {
            return;
        }
        TextChannel channel = Bot.jda.getTextChannelById(channelId);
        if (channel == null) {
            return;
        }

        List<TrackedPlayer> sorted = new ArrayList<>(players);
        sorted.sort(Comparator.comparing(p -> p.username.toLowerCase()));

        List<MessageEmbed> embeds = new ArrayList<>();
        for (TrackedPlayer player : sorted) {
            pollStatus(player);
            embeds.add(buildEmbed(player));
            if (embeds.size() >= MAX_PLAYERS_PER_MESSAGE) {
                break; // Discord allows at most 10 embeds per message - rest just won't show
            }
        }

        String trackerKey = trackerKey(channelId);
        String existingId = ChannelMessageTracker.get(trackerKey);

        try {
            if (existingId != null) {
                try {
                    channel.editMessageEmbedsById(existingId, embeds).complete();
                    return;
                } catch (Exception editError) {
                    // The message was deleted by someone/something else - fall through and post a fresh one.
                    ChannelMessageTracker.forget(trackerKey);
                }
            }
            Message sent = channel.sendMessageEmbeds(embeds).complete();
            ChannelMessageTracker.remember(trackerKey, sent.getId());
        } catch (Exception e) {
            Log.error("Failed to update player tracker message for channel " + channelId + ": " + e.getMessage());
        }
    }

    private static void pollStatus(TrackedPlayer player) {
        try {
            OnlineStatus status = OnlineChecker.checkByUuid(player.uuid);
            boolean wasOnline = player.statusKnown && player.online;
            player.online = status.online;
            player.gameType = status.gameType;
            player.mode = status.mode;
            player.statusKnown = true;
            if (status.online) {
                player.offlineSince = null;
            } else if (wasOnline || player.offlineSince == null) {
                player.offlineSince = Instant.now();
            }
        } catch (Exception e) {
            Log.warn("Couldn't refresh status for " + player.username + ": " + e.getMessage());
        }
    }

    private static MessageEmbed buildEmbed(TrackedPlayer player) {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(player.username)
                .setThumbnail("https://mc-heads.net/avatar/" + player.uuid + "/100");

        if (!player.statusKnown) {
            eb.setDescription("\u26aa Status unknown yet").setColor(Color.GRAY);
        } else if (player.online) {
            String where = player.gameType != null ? player.gameType : "Hypixel";
            if (player.mode != null) {
                where += " (" + player.mode + ")";
            }
            eb.setDescription("\ud83d\udfe2 **Online** - " + where).setColor(Color.GREEN);
        } else {
            String since = player.offlineSince != null
                    ? DiscordTimestamps.relative(player.offlineSince)
                    : "unknown";
            eb.setDescription("\ud83d\udd34 **Offline** - since " + since).setColor(Color.DARK_GRAY);
        }

        return eb.build();
    }
}
