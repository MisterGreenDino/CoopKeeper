//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.bot.utils;

import java.awt.Color;
import java.time.Instant;
import java.util.Collection;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.bot.reminder.Reminder;
import org.bot.reminder.ReminderManager;

public class CommandListener extends ListenerAdapter {
    public CommandListener() {
    }

    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "reminder" -> this.handleCreate(event);
            case "reminder_list" -> this.handleList(event);
            case "reminder_delete" -> this.handleDelete(event);
            case "reminder_reset" -> this.handleReset(event);
            case "reminder_timer" -> this.handleTimer(event);
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
