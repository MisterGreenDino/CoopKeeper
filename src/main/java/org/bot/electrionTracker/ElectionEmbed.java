package org.bot.electrionTracker;

import net.dv8tion.jda.api.EmbedBuilder;
import org.bot.eventTracker.SkyBlockCalendar;
import org.bot.utils.DiscordTimestamps;

import java.awt.Color;
import java.time.Instant;

/** Builds the "Mayor Election" embed content shared by the one-shot /election command and ElectionBoard. */
public final class ElectionEmbed {

    private ElectionEmbed() {
    }

    public static EmbedBuilder build(Mayor mayor, Election election) {
        EmbedBuilder eb = new EmbedBuilder().setTitle("\ud83d\uddf3\ufe0f Mayor Election").setColor(Color.MAGENTA);

        eb.addField("Current Mayor", mayor != null ? mayor.name : "Unknown", true);
        eb.addField("Current Minister", mayor != null && mayor.minister != null ? mayor.minister : "None", true);

        // The winning candidate takes office at Late Spring 27th - NOT at the SkyBlock New Year -
        // confirmed on the Wiki: "The election will end on 27th of Late Spring 00:00 the following
        // year, at which point the candidate with the most votes will become the mayor."
        Instant newMayorAt = SkyBlockCalendar.nextOccurrenceOfDate(Instant.now(), 2, 27);
        eb.addField("New Mayor", DiscordTimestamps.relativeAndTime(newMayorAt), false);

        if (mayor != null && !mayor.perks.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Perk perk : mayor.perks) {
                sb.append("**").append(perk.name).append("**");
                if (perk.description != null && !perk.description.isBlank()) {
                    sb.append(": ").append(perk.description);
                }
                sb.append("\n");
            }
            eb.addField("Perks", sb.toString(), false);
        }

        StringBuilder specials = new StringBuilder();
        for (SpecialMayors.SpecialMayor special : SpecialMayors.ALL) {
            Instant nextAppearance = SpecialMayors.nextAppearance(special, Instant.now());
            specials.append("**").append(special.name()).append("**: ")
                    .append(DiscordTimestamps.relativeAndTime(nextAppearance)).append("\n");
        }
        eb.addField("\u2b50 Special Mayors - Next Appearance", specials.toString(), false);

        if (election != null) {
            eb.setFooter("Ongoing election, Year " + election.year);
        }

        return eb;
    }
}
