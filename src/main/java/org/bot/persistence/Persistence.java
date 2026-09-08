package org.bot.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bot.electrionTracker.ElectionBoard;
import org.bot.electrionTracker.MayorWatcher;
import org.bot.eventTracker.EventTracker;
import org.bot.onlineChecker.PlayerTracker;
import org.bot.reminder.ReminderManager;
import org.bot.reminder.ReminderMenu;
import org.bot.utils.ChannelMessageTracker;
import org.bot.utils.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Persists which channels track what (events, mayor changes, tracked players, reminders) to a
 * small JSON file on disk (./data/bot-state.json), so restarting the bot doesn't wipe out
 * everyone's /..._track setup.
 *
 * {@link #load()} should be called once at startup. {@link #save()} should be called after any
 * tracking mutation (add/remove/track/untrack) so the file stays in sync - it's cheap and safe
 * to call often (synchronized, overwrites the whole file each time).
 */
public final class Persistence {

    private static final Path FILE = Path.of("data", "bot-state.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Persistence() {
    }

    public static synchronized void load() {
        if (!Files.exists(FILE)) {
            Log.info("No saved state found at " + FILE + " (first run?) - starting fresh.");
            return;
        }
        try {
            String json = Files.readString(FILE);
            BotState state = GSON.fromJson(json, BotState.class);
            if (state == null) {
                return;
            }

            if (state.events != null) {
                EventTracker.restore(state.events);
            }
            if (state.mayorChannels != null) {
                for (String channelId : state.mayorChannels) {
                    MayorWatcher.track(channelId);
                }
            }
            if (state.electionChannels != null) {
                for (String channelId : state.electionChannels) {
                    ElectionBoard.track(channelId);
                }
            }
            if (state.playerTracker != null) {
                PlayerTracker.restore(state.playerTracker);
            }
            if (state.reminders != null) {
                ReminderManager.restore(state.reminders);
            }
            if (state.messageIds != null) {
                ChannelMessageTracker.restore(state.messageIds);
            }

            // A message left over from before a restart may be showing minutes/hours-old data
            // (or worse, a stale "Dark Auction is starting now!" ping). Rather than silently
            // trusting/editing it, clear it out and post fresh content immediately for anything
            // that has current content to show (live boards) - ping-style trackers (events,
            // mayor) just get their stale ping cleaned up; they'll post again next real trigger.
            PlayerTracker.hardResetAllOnStartup();
            ElectionBoard.hardResetAllOnStartup();
            if (state.messageIds != null) {
                for (String key : state.messageIds.keySet()) {
                    String channelId = ReminderMenu.channelIdFromKey(key);
                    if (channelId != null) {
                        ReminderMenu.hardResetOnStartup(channelId);
                    }
                }
            }

            Log.info("Restored saved state from " + FILE);
        } catch (Exception e) {
            Log.error("Failed to load saved state (" + FILE + "): " + e.getMessage());
        }
    }

    public static synchronized void save() {
        try {
            BotState state = new BotState();
            state.events = EventTracker.exportState();
            state.mayorChannels = new ArrayList<>(MayorWatcher.getTrackedChannels());
            state.electionChannels = new ArrayList<>(ElectionBoard.getTrackedChannels());
            state.playerTracker = PlayerTracker.exportState();
            state.reminders = ReminderManager.exportState();
            state.messageIds = ChannelMessageTracker.exportState();

            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(state));
        } catch (IOException e) {
            Log.error("Failed to save state (" + FILE + "): " + e.getMessage());
        }
    }
}
