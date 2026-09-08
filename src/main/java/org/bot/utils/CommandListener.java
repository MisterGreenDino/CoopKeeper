package org.bot.utils;

import java.awt.Color;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.bot.electrionTracker.Election;
import org.bot.electrionTracker.ElectionBoard;
import org.bot.electrionTracker.ElectionEmbed;
import org.bot.electrionTracker.ElectionTracker;
import org.bot.electrionTracker.Mayor;
import org.bot.electrionTracker.MayorWatcher;
import org.bot.electrionTracker.VoteChartGenerator;
import org.bot.eventTracker.EventScheduler;
import org.bot.eventTracker.EventTracker;
import org.bot.eventTracker.EventType;
import org.bot.eventTracker.SkyBlockCalendar;
import org.bot.eventTracker.SkyblockEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bot.onlineChecker.OnlineChecker;
import org.bot.onlineChecker.OnlineStatus;
import org.bot.onlineChecker.PlayerTracker;
import org.bot.persistence.Persistence;
import org.bot.profileChecker.PlayerProfile;
import org.bot.profileChecker.ProfileChecker;
import org.bot.reminder.Reminder;
import org.bot.reminder.ReminderManager;
import org.bot.reminder.ReminderMenu;
import org.bot.skillXPCalculator.SkillType;
import org.bot.skillXPCalculator.SkillXPCalculator;

public class CommandListener extends ListenerAdapter {

    // Hypixel/Mojang calls are blocking - run them off the JDA gateway thread.
    private static final ExecutorService WORKERS = Executors.newCachedThreadPool();

    public CommandListener() {
    }

    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "reminder" -> this.handleCreate(event);
            case "reminder_list" -> this.handleList(event);
            case "reminder_delete" -> this.handleDelete(event);
            case "reminder_reset" -> this.handleReset(event);
            case "reminder_timer" -> this.handleTimer(event);
            case "skyblock" -> this.handleSkyblock(event);
            case "online" -> this.handleOnline(event);
            case "election" -> this.handleElection(event);
            case "election_track" -> this.handleElectionTrack(event);
            case "election_untrack" -> this.handleElectionUntrack(event);
            case "skillxp" -> this.handleSkillXp(event);
            case "darkauction_track" -> this.handleDarkAuctionTrack(event);
            case "darkauction_untrack" -> this.handleDarkAuctionUntrack(event);
            case "events_track_all" -> this.handleEventsTrackAll(event);
            case "events_untrack_all" -> this.handleEventsUntrackAll(event);
            case "events_upcoming" -> this.handleEventsUpcoming(event);
            case "mayor_track" -> this.handleMayorTrack(event);
            case "mayor_untrack" -> this.handleMayorUntrack(event);
            case "reminders" -> this.handleReminders(event);
            case "player_tracker" -> this.handlePlayerTracker(event);
            default -> event.reply("Unknown command").queue();
        }

    }

    private void handleCreate(SlashCommandInteractionEvent event) {
        OptionMapping title = event.getOption("title");
        OptionMapping interval = event.getOption("interval");
        if (title != null && interval != null) {
            Reminder r = ReminderManager.create(title.getAsString(), event.getChannelId(), interval.getAsString());
            Persistence.save();
            EmbedBuilder eb = (new EmbedBuilder()).setTitle("\ud83d\udd14 Reminder Created").setColor(Color.ORANGE).addField("Task", r.title, false).addField("ID", r.id, false).addField("Created", Instant.now().toString(), false);
            ((ReplyCallbackAction)event.replyEmbeds(eb.build(), new MessageEmbed[0]).addActionRow(new ItemComponent[]{Button.success("done:" + r.id, "✔ Done")})).queue();
        } else {
            event.reply("Missing arguments").queue();
        }
    }

    private void handleList(SlashCommandInteractionEvent event) {
        Collection<Reminder> all = ReminderManager.getAll();
        if (all.isEmpty()) {
            event.reply("No reminders").queue();
        } else {
            EmbedBuilder eb = (new EmbedBuilder()).setTitle("\ud83d\udccb Active Reminders").setColor(Color.BLUE);

            for(Reminder r : all) {
                eb.addField(r.title, "ID: `" + r.id + "`", false);
            }

            event.replyEmbeds(eb.build(), new MessageEmbed[0]).queue();
        }
    }

    private void handleDelete(SlashCommandInteractionEvent event) {
        OptionMapping opt = event.getOption("id");
        if (opt == null) {
            event.reply("Missing ID").queue();
        } else {
            Reminder removed = ReminderManager.delete(opt.getAsString());
            if (removed == null) {
                event.reply("Not found").queue();
            } else {
                Persistence.save();
                event.reply("Deleted: " + removed.title).queue();
            }
        }
    }

    private void handleReset(SlashCommandInteractionEvent event) {
        OptionMapping opt = event.getOption("id");
        if (opt == null) {
            event.reply("Missing ID").queue();
        } else {
            Reminder r = ReminderManager.resolve(opt.getAsString());
            if (r == null) {
                event.reply("Reminder not found").queue();
            } else {
                ReminderManager.reset(r.id);
                Persistence.save();
                event.reply("Reset ✔ `" + r.title + "`").queue();
            }
        }
    }

    private void handleTimer(SlashCommandInteractionEvent event) {
        OptionMapping opt = event.getOption("id");
        if (opt == null) {
            event.reply("Missing ID").queue();
        } else {
            Reminder r = ReminderManager.resolve(opt.getAsString());
            if (r == null) {
                event.reply("Reminder not found").queue();
            } else {
                long now = System.currentTimeMillis();
                long remaining = r.nextTrigger.toEpochMilli() - now;
                event.reply("Next trigger in: `" + TimeParser.format(Math.max(0L, remaining)) + "`").queue();
            }
        }
    }

    // ---- SkyBlock / Hypixel commands ----
    // All of these hit the Hypixel/Mojang APIs, so we defer the reply and do the work off-thread.

    private void handleSkyblock(SlashCommandInteractionEvent event) {
        String username = event.getOption("username").getAsString();
        event.deferReply().queue();
        WORKERS.submit(() -> {
            try {
                PlayerProfile profile = ProfileChecker.getByUsername(username);
                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle("\ud83c\udfdd\ufe0f SkyBlock - " + profile.username)
                        .setColor(Color.GREEN)
                        .addField("Profile", profile.selected != null ? profile.selected.toString() : "Unknown", true)
                        .addField("Skill Average", String.format("%.2f", profile.skillAverage), true)
                        .addField("Purse", String.format("%,.0f coins", profile.purse), true)
                        .addField("Bank", String.format("%,.0f coins", profile.bank), true);
                event.getHook().sendMessageEmbeds(eb.build()).queue();
            } catch (Exception e) {
                Log.error("skyblock command failed: " + e.getMessage());
                event.getHook().sendMessage("Couldn't fetch that player's SkyBlock data: " + e.getMessage()).queue();
            }
        });
    }

    private void handleOnline(SlashCommandInteractionEvent event) {
        String username = event.getOption("username").getAsString();
        event.deferReply().queue();
        WORKERS.submit(() -> {
            try {
                OnlineStatus status = OnlineChecker.checkByUsername(username);
                event.getHook().sendMessage("**" + username + "** - " + status).queue();
            } catch (Exception e) {
                Log.error("online command failed: " + e.getMessage());
                event.getHook().sendMessage("Couldn't check that player's status: " + e.getMessage()).queue();
            }
        });
    }

    private void handleElection(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        WORKERS.submit(() -> {
            try {
                Mayor mayor = ElectionTracker.getCurrentMayor();
                Election election = ElectionTracker.getOngoingElection();
                EmbedBuilder eb = ElectionEmbed.build(mayor, election);

                if (election != null && !election.candidates.isEmpty()) {
                    try {
                        byte[] chart = VoteChartGenerator.renderVotes(election.candidates);
                        eb.setImage("attachment://votes.png");
                        event.getHook().sendMessageEmbeds(eb.build())
                                .addFiles(FileUpload.fromData(chart, "votes.png"))
                                .queue();
                    } catch (Exception chartError) {
                        Log.error("Vote chart render failed, sending embed without it: " + chartError.getMessage());
                        event.getHook().sendMessageEmbeds(eb.build()).queue();
                    }
                } else {
                    event.getHook().sendMessageEmbeds(eb.build()).queue();
                }
            } catch (Exception e) {
                Log.error("election command failed: " + e.getMessage());
                event.getHook().sendMessage("Couldn't fetch the election: " + e.getMessage()).queue();
            }
        });
    }

    private void handleElectionTrack(SlashCommandInteractionEvent event) {
        ElectionBoard.track(event.getChannelId());
        Persistence.save();
        event.reply("\u2705 I'll keep an auto-updating election panel here, refreshed every 5 minutes.").queue();
    }

    private void handleElectionUntrack(SlashCommandInteractionEvent event) {
        ElectionBoard.untrack(event.getChannelId());
        Persistence.save();
        event.reply("\u2705 Stopped the election panel in this channel.").queue();
    }

    private void handleSkillXp(SlashCommandInteractionEvent event) {
        String username = event.getOption("username").getAsString();
        String skillInput = event.getOption("skill").getAsString();
        event.deferReply().queue();
        WORKERS.submit(() -> {
            try {
                SkillType type;
                try {
                    type = SkillType.valueOf(skillInput.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    event.getHook().sendMessage("Unknown skill `" + skillInput + "`. Valid: " +
                            List.of(SkillType.values())).queue();
                    return;
                }

                PlayerProfile profile = ProfileChecker.getByUsername(username);
                Double level = profile.skillLevels.get(type);
                if (level == null) {
                    event.getHook().sendMessage("No " + type + " data found for **" + username +
                            "** - their Skills API setting might be off.").queue();
                } else {
                    event.getHook().sendMessage("**" + username + "**'s " + type + " level: `" +
                            String.format("%.2f", level) + "`").queue();
                }
            } catch (Exception e) {
                Log.error("skillxp command failed: " + e.getMessage());
                event.getHook().sendMessage("Couldn't fetch skill data: " + e.getMessage()).queue();
            }
        });
    }

    private void handleDarkAuctionTrack(SlashCommandInteractionEvent event) {
        EventTracker.track(EventType.DARK_AUCTION, event.getChannelId());
        Persistence.save();
        event.reply("\u2705 I'll announce every Dark Auction start in this channel from now on.").queue();
    }

    private void handleDarkAuctionUntrack(SlashCommandInteractionEvent event) {
        boolean removed = EventTracker.untrack(EventType.DARK_AUCTION, event.getChannelId());
        if (removed) {
            Persistence.save();
        }
        event.reply(removed
                ? "\u2705 Stopped announcing Dark Auctions here."
                : "Dark Auction wasn't being tracked in this channel.").queue();
    }

    private void handleEventsTrackAll(SlashCommandInteractionEvent event) {
        Collection<SkyblockEvent> tracked = EventTracker.trackAll(event.getChannelId());
        Persistence.save();
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("\u2705 Now tracking " + tracked.size() + " SkyBlock event(s) in this channel")
                .setColor(Color.ORANGE);
        for (SkyblockEvent e : tracked) {
            eb.addField(e.label, "Next: " + DiscordTimestamps.relativeAndTime(e.nextTrigger), false);
        }
        event.replyEmbeds(eb.build(), new MessageEmbed[0]).queue();
    }

    private void handleEventsUntrackAll(SlashCommandInteractionEvent event) {
        int removed = EventTracker.untrackAll(event.getChannelId());
        if (removed > 0) {
            Persistence.save();
        }
        event.reply(removed > 0
                ? "\u2705 Stopped tracking " + removed + " SkyBlock event(s) in this channel."
                : "No SkyBlock events were being tracked in this channel.").queue();
    }

    private void handleEventsUpcoming(SlashCommandInteractionEvent event) {
        Instant now = Instant.now();
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("\ud83d\uddd3\ufe0f Upcoming SkyBlock Events")
                .setColor(Color.CYAN)
                .setFooter("SkyBlock time: " + SkyBlockCalendar.describe(now));

        for (EventType type : EventType.values()) {
            if (type == EventType.CUSTOM) {
                continue;
            }
            Instant next = EventScheduler.nextTrigger(type, now, -1);
            eb.addField(type.displayName, DiscordTimestamps.relativeAndTime(next), false);
        }

        event.replyEmbeds(eb.build(), new MessageEmbed[0]).queue();
    }

    private void handleMayorTrack(SlashCommandInteractionEvent event) {
        MayorWatcher.track(event.getChannelId());
        Persistence.save();
        event.reply("\u2705 I'll announce it here whenever the SkyBlock mayor changes.").queue();
    }

    private void handleMayorUntrack(SlashCommandInteractionEvent event) {
        MayorWatcher.untrack(event.getChannelId());
        Persistence.save();
        event.reply("\u2705 Stopped announcing mayor changes here.").queue();
    }

    private void handleReminders(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        WORKERS.submit(() -> {
            ReminderMenu.refresh(event.getChannelId());
            event.getHook().sendMessage("\u2705 Reminders panel is ready below.").queue();
        });
    }

    private void handlePlayerTracker(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if ("clear".equals(sub)) {
            int removed = PlayerTracker.clear(event.getChannelId());
            if (removed > 0) {
                Persistence.save();
            }
            event.reply(removed > 0
                    ? "\u2705 Stopped tracking " + removed + " player(s) in this channel."
                    : "No players were being tracked in this channel.").queue();
            return;
        }

        OptionMapping usernameOpt = event.getOption("username");
        if (sub == null || usernameOpt == null) {
            event.reply("Missing arguments").queue();
            return;
        }
        String username = usernameOpt.getAsString();
        event.deferReply().queue();
        WORKERS.submit(() -> {
            try {
                if ("add".equals(sub)) {
                    PlayerTracker.add(event.getChannelId(), username);
                    Persistence.save();
                    event.getHook().sendMessage("\u2705 Now tracking **" + username +
                            "** here - the status board updates every minute.").queue();
                } else if ("remove".equals(sub)) {
                    boolean removed = PlayerTracker.remove(event.getChannelId(), username);
                    if (removed) {
                        Persistence.save();
                    }
                    event.getHook().sendMessage(removed
                            ? "\u2705 Stopped tracking **" + username + "**."
                            : "**" + username + "** wasn't being tracked in this channel.").queue();
                } else {
                    event.getHook().sendMessage("Unknown subcommand").queue();
                }
            } catch (Exception e) {
                Log.error("player_tracker command failed: " + e.getMessage());
                event.getHook().sendMessage("Error: " + e.getMessage()).queue();
            }
        });
    }

    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (id == null) {
            return;
        }

        if (id.startsWith("done:")) {
            String reminderId = id.substring("done:".length());
            Reminder r = ReminderManager.get(reminderId);
            if (r == null) {
                event.reply("Already removed").setEphemeral(true).queue();
            } else {
                ReminderManager.ack(reminderId);
                Persistence.save();
                ReminderMenu.refresh(r.channelId);
                event.reply("✔ Completed").queue();
            }
        } else if (id.equals(ReminderMenu.NEW_BUTTON_ID)) {
            TextInput title = TextInput.create("title", "Title", TextInputStyle.SHORT)
                    .setPlaceholder("e.g. Restock the shop").setRequired(true).build();
            TextInput interval = TextInput.create("interval", "Interval (e.g. 10s, 5m, 2h)", TextInputStyle.SHORT)
                    .setPlaceholder("2h").setRequired(true).build();
            Modal modal = Modal.create("reminder_menu_new_modal", "New Reminder")
                    .addActionRow(title).addActionRow(interval).build();
            event.replyModal(modal).queue();
        } else if (id.startsWith(ReminderMenu.RESET_PREFIX)) {
            String reminderId = id.substring(ReminderMenu.RESET_PREFIX.length());
            Reminder r = ReminderManager.resolve(reminderId);
            if (r != null) {
                ReminderManager.reset(r.id);
                Persistence.save();
                ReminderMenu.refresh(r.channelId);
            }
            event.reply(r != null ? "\u2705 Reset **" + r.title + "**." : "That reminder no longer exists.")
                    .setEphemeral(true).queue();
        } else if (id.startsWith(ReminderMenu.DELETE_PREFIX)) {
            String reminderId = id.substring(ReminderMenu.DELETE_PREFIX.length());
            Reminder r = ReminderManager.delete(reminderId);
            if (r != null) {
                Persistence.save();
                ReminderMenu.refresh(r.channelId);
            }
            event.reply(r != null ? "\u2705 Deleted **" + r.title + "**." : "That reminder no longer exists.")
                    .setEphemeral(true).queue();
        } else if (id.equals(ReminderMenu.CANCEL_ID)) {
            event.reply("Cancelled.").setEphemeral(true).queue();
        }
    }

    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!ReminderMenu.SELECT_ID.equals(event.getComponentId())) {
            return;
        }
        String reminderId = event.getValues().isEmpty() ? null : event.getValues().get(0);
        Reminder r = reminderId != null ? ReminderManager.resolve(reminderId) : null;
        if (r == null) {
            event.reply("That reminder no longer exists - try refreshing with /reminders.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(r.title)
                .setColor(Color.ORANGE)
                .addField("Next trigger", DiscordTimestamps.relativeAndTime(r.nextTrigger), false);

        event.replyEmbeds(eb.build())
                .setEphemeral(true)
                .addActionRow(
                        Button.primary(ReminderMenu.RESET_PREFIX + r.id, "\ud83d\udd04 Reset"),
                        Button.danger(ReminderMenu.DELETE_PREFIX + r.id, "\ud83d\uddd1\ufe0f Delete"),
                        Button.secondary(ReminderMenu.CANCEL_ID, "Cancel")
                )
                .queue();
    }

    public void onModalInteraction(ModalInteractionEvent event) {
        if (!"reminder_menu_new_modal".equals(event.getModalId())) {
            return;
        }
        String title = event.getValue("title") != null ? event.getValue("title").getAsString() : null;
        String interval = event.getValue("interval") != null ? event.getValue("interval").getAsString() : null;
        if (title == null || interval == null) {
            event.reply("Missing fields.").setEphemeral(true).queue();
            return;
        }
        try {
            Reminder r = ReminderManager.create(title, event.getChannelId(), interval);
            Persistence.save();
            ReminderMenu.refresh(event.getChannelId());
            event.reply("\u2705 Created **" + r.title + "**.").setEphemeral(true).queue();
        } catch (Exception e) {
            event.reply("Error: " + e.getMessage()).setEphemeral(true).queue();
        }
    }
}
