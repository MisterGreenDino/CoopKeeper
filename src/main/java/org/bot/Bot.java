//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.bot;

import java.util.List;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bot.utils.CommandListener;

public class Bot {
    public static JDA jda;
    private static final String GUILD_ID = (String)System.getenv().getOrDefault("GUILD_ID", "1515375020887642255");

    public Bot() {
    }

    public void start() {
        String token = System.getenv("DISCORD_TOKEN");
        if (token != null && !token.isBlank()) {
            jda = JDABuilder.createDefault(token).addEventListeners(new Object[]{new CommandListener()}).build();

            try {
                jda.awaitReady();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            this.registerCommands();
            System.out.println("Bot online");
        } else {
            throw new IllegalStateException("DISCORD_TOKEN missing");
        }
    }

    private void registerCommands() {
        Guild guild = jda.getGuildById(GUILD_ID);
        if (guild == null) {
            throw new IllegalStateException("Guild not found: " + GUILD_ID);
        } else {
            this.wipeAllCommands(guild);
            List<CommandData> commands = List.of(Commands.slash("reminder", "Create reminder").addOption(OptionType.STRING, "title", "Task title", true).addOption(OptionType.STRING, "interval", "10s / 5m / 2h", true), Commands.slash("reminder_list", "List reminders"), Commands.slash("reminder_delete", "Delete reminder (name or id)").addOption(OptionType.STRING, "id", "Name or ID", true), Commands.slash("reminder_reset", "Reset reminder (name or id)").addOption(OptionType.STRING, "id", "Name or ID", true), Commands.slash("reminder_timer", "Show timer (name or id)").addOption(OptionType.STRING, "id", "Name or ID", true));
            guild.updateCommands().addCommands(commands).queue((s) -> System.out.println("Commands synced"), (e) -> System.err.println("Command sync failed: " + e.getMessage()));
        }
    }

    private void wipeAllCommands(Guild guild) {
        guild.updateCommands().complete();
        jda.updateCommands().complete();
        System.out.println("All Discord commands wiped");
    }
}
