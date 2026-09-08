package org.bot.utils;

import java.awt.Color;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.bot.electrionTracker.Candidate;
import org.bot.electrionTracker.Election;
import org.bot.electrionTracker.ElectionTracker;
import org.bot.electrionTracker.Mayor;
import org.bot.eventTracker.EventTracker;
import org.bot.eventTracker.EventType;
import org.bot.eventTracker.SkyBlockCalendar;
import org.bot.eventTracker.SkyblockEvent;
import org.bot.onlineChecker.OnlineChecker;
import org.bot.onlineChecker.OnlineStatus;
import org.bot.profileChecker.PlayerProfile;
import org.bot.profileChecker.ProfileChecker;
import org.bot.reminder.Reminder;
import org.bot.reminder.ReminderManager;
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
            case "skillxp" -> this.handleSkillXp(event);
            case "darkauction_track" -> this.handleDarkAuctionTrack(event);
            case "events_track_all" -> this.handleEventsTrackAll(event);
            case "events_upcoming" -> this.handleEventsUpcoming(event);
            default -> event.reply("Unknown command").queue();
        }

    }

    private void handleCreate(SlashCommandInteractionEvent event) {
        OptionMapping title = event.getOption("title");
        OptionMapping interval = event.getOption("interval");
        if (title != null && interval != null) {
            Reminder r = ReminderManager.create(title.getAsString(), event.getChannelId(), interval.getAsString());
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

                EmbedBuilder eb = new EmbedBuilder().setTitle("\ud83d\uddf3\ufe0f SkyBlock Election").setColor(Color.MAGENTA);

                if (mayor != null) {
                    String perks = mayor.perks.isEmpty() ? "None" : String.join(", ", mayor.perks);
                    eb.addField("Current Mayor", mayor.name, false).addField("Active Perks", perks, false);
                } else {
                    eb.addField("Current Mayor", "Unknown", false);
                }

                if (election != null && !election.candidates.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (Candidate c : election.candidates) {
                        sb.append("**").append(c.name).append("** - ").append(c.votes).append(" votes\n");
                    }
                    eb.addField("Ongoing Election (Year " + election.year + ")", sb.toString(), false);
                }

                event.getHook().sendMessageEmbeds(eb.build()).queue();
            } catch (Exception e) {
                Log.error("election command failed: " + e.getMessage());
                event.getHook().sendMessage("Couldn't fetch the election: " + e.getMessage()).queue();
            }
        });
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
        EventTracker.trackDarkAuction(event.getChannelId());
        event.reply("\u2705 I'll announce every Dark Auction start in this channel from now on.").queue();
    }

    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (id != null && id.startsWith("done:")) {
            String reminderId = id.substring(5);
            Reminder r = ReminderManager.get(reminderId);
            if (r == null) {
                event.reply("Already removed").setEphemeral(true).queue();
            } else {
                ReminderManager.ack(reminderId);
                event.reply("✔ Completed").queue();
            }
        }
    }
}
