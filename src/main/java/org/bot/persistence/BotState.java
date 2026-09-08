package org.bot.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plain data holder serialized to/from JSON by {@link Persistence}. Only the minimum needed to
 * re-derive tracking state is stored - e.g. reminder/event *next trigger* is recomputed from
 * "now" on restart rather than trusted from a possibly-stale saved timestamp.
 */
public class BotState {

    public List<TrackedEvent> events = new ArrayList<>();
    public List<String> mayorChannels = new ArrayList<>();
    public List<String> electionChannels = new ArrayList<>();
    public Map<String, List<TrackedPlayerData>> playerTracker = new HashMap<>();
    public List<ReminderData> reminders = new ArrayList<>();
    /** From ChannelMessageTracker: covers event pings, mayor pings, election panel, player tracker board, reminders panel. */
    public Map<String, String> messageIds = new HashMap<>();

    public record TrackedEvent(String type, String channelId, String label, long customPeriodMs) {
    }

    public record TrackedPlayerData(String username, String uuid) {
    }

    public record ReminderData(String id, String name, String title, String channelId,
                                long intervalMs, long nextTriggerEpochMs, boolean waitingAck) {
    }
}
