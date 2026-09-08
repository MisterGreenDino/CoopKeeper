package org.bot.reminder;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.bot.Bot;
import org.bot.utils.ChannelMessageTracker;
import org.bot.utils.DiscordTimestamps;
import org.bot.utils.Log;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single auto-updating "panel" message per channel for managing reminders without having to type
 * slash commands: a dropdown to pick a reminder (which replies with Reset/Delete buttons, just
 * for the person who opened it) and a button to create a new one via a modal form. Edited in
 * place - no @mention involved, so there's no need to repost a brand new message every time.
 *
 * Like PlayerTracker, all message send/edit decisions run on a dedicated single-threaded executor
 * using BLOCKING (.complete()) calls, so two near-simultaneous refreshes for the same channel
 * (e.g. a button click and a modal submission landing at the same time) can't both decide "no
 * message yet" and each post a duplicate.
 */
public final class ReminderMenu {

    public static final String SELECT_ID = "reminder_menu_select";
    public static final String NEW_BUTTON_ID = "reminder_menu_new";
    public static final String RESET_PREFIX = "reminder_action_reset:";
    public static final String DELETE_PREFIX = "reminder_action_delete:";
    public static final String CANCEL_ID = "reminder_action_cancel";

    private static final String KEY_PREFIX = "reminder_panel:";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ReminderMenu() {
    }

    private static String key(String channelId) {
        return KEY_PREFIX + channelId;
    }

    /** Channel id this panel key belongs to, or null if the key isn't a reminder panel key. */
    public static String channelIdFromKey(String key) {
        return key.startsWith(KEY_PREFIX) ? key.substring(KEY_PREFIX.length()) : null;
    }

    /** Builds/updates the panel message for this channel. Safe to call often (e.g. after every change). */
    public static void refresh(String channelId) {
        executor.execute(() -> doRefresh(channelId));
    }

    /** Deletes any stale panel left over from before a restart, then posts a fresh one. */
    public static void hardResetOnStartup(String channelId) {
        executor.execute(() -> {
            if (Bot.jda == null) {
                return;
            }
            TextChannel channel = Bot.jda.getTextChannelById(channelId);
            if (channel != null) {
                ChannelMessageTracker.deletePrevious(channel, key(channelId));
            }
            ChannelMessageTracker.forget(key(channelId));
            doRefresh(channelId);
        });
    }

    private static void doRefresh(String channelId) {
        if (Bot.jda == null) {
            return;
        }
        TextChannel channel = Bot.jda.getTextChannelById(channelId);
        if (channel == null) {
            return;
        }

        List<Reminder> reminders = new ArrayList<>(ReminderManager.getAll());
        reminders.removeIf(r -> !r.channelId.equals(channelId));
        reminders.sort(Comparator.comparing(r -> r.title.toLowerCase()));

        MessageEmbed embed = buildEmbed(reminders);
        List<ActionRow> rows = buildComponents(reminders);
        String trackerKey = key(channelId);
        String existingId = ChannelMessageTracker.get(trackerKey);

        try {
            if (existingId != null) {
                try {
                    channel.editMessageEmbedsById(existingId, embed).setComponents(rows).complete();
                    return;
                } catch (Exception editError) {
                    ChannelMessageTracker.forget(trackerKey);
                }
            }
            Message sent = channel.sendMessageEmbeds(embed).setComponents(rows).complete();
            ChannelMessageTracker.remember(trackerKey, sent.getId());
        } catch (Exception e) {
            Log.error("Failed to refresh reminders panel for channel " + channelId + ": " + e.getMessage());
        }
    }

    private static MessageEmbed buildEmbed(List<Reminder> reminders) {
        EmbedBuilder eb = new EmbedBuilder().setTitle("\u23f0 Reminders").setColor(Color.ORANGE);
        if (reminders.isEmpty()) {
            eb.setDescription("No reminders in this channel yet - click **New Reminder** to add one.");
        } else {
            for (Reminder r : reminders) {
                String status = r.waitingAck
                        ? "\u26a0\ufe0f Waiting to be marked done"
                        : "Next: " + DiscordTimestamps.relativeAndTime(r.nextTrigger);
                eb.addField(r.title, status + "\nID: `" + r.id + "`", false);
            }
            eb.setFooter("Pick a reminder below to reset or delete it.");
        }
        return eb.build();
    }

    private static List<ActionRow> buildComponents(List<Reminder> reminders) {
        List<ActionRow> rows = new ArrayList<>();
        if (!reminders.isEmpty()) {
            StringSelectMenu.Builder menu = StringSelectMenu.create(SELECT_ID)
                    .setPlaceholder("Manage a reminder...");
            for (Reminder r : reminders.subList(0, Math.min(reminders.size(), 25))) {
                menu.addOption(r.title, r.id);
            }
            rows.add(ActionRow.of(menu.build()));
        }
        rows.add(ActionRow.of(Button.success(NEW_BUTTON_ID, "\u2795 New Reminder")));
        return rows;
    }
}
