//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.bot.reminder;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.bot.Bot;
import org.bot.utils.TimeParser;

public class ReminderManager {
    private static final Map<String, Reminder> byId = new ConcurrentHashMap();
    private static final Map<String, Reminder> byName = new ConcurrentHashMap();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ReminderManager() {
    }

    public static Reminder create(String title, String channelId, String interval) {
        if (byName.containsKey(title.toLowerCase())) {
            throw new IllegalArgumentException("Reminder name already exists");
        } else {
            Reminder r = new Reminder();
            r.id = UUID.randomUUID().toString();
            r.name = title.toLowerCase();
            r.title = title;
            r.channelId = channelId;
            r.intervalMs = TimeParser.parse(interval);
            r.nextTrigger = Instant.now().plusMillis(r.intervalMs);
            byId.put(r.id, r);
            byName.put(r.name, r);
            return r;
        }
    }

    public static Reminder get(String id) {
        return (Reminder)byId.get(id);
    }

    public static Reminder resolve(String key) {
        if (key == null) {
            return null;
        } else {
            Reminder r = (Reminder)byId.get(key);
            return r != null ? r : (Reminder)byName.get(key.toLowerCase());
        }
    }

    public static Collection<Reminder> getAll() {
        return byId.values();
    }

    public static Reminder delete(String key) {
        Reminder r = resolve(key);
        if (r == null) {
            return null;
        } else {
            byId.remove(r.id);
            byName.remove(r.name);
            return r;
        }
    }

    public static void ack(String id) {
        Reminder r = (Reminder)byId.get(id);
        if (r != null) {
            r.waitingAck = false;
            r.nextTrigger = Instant.now().plusMillis(r.intervalMs);
        }
    }

    public static void reset(String id) {
        Reminder r = (Reminder)byId.get(id);
        if (r != null) {
            r.waitingAck = false;
            r.nextTrigger = Instant.now().plusMillis(r.intervalMs);
        }
    }

    private static void tick() {
        long now = System.currentTimeMillis();

        for(Reminder r : byId.values()) {
            if (!r.waitingAck && now >= r.nextTrigger.toEpochMilli()) {
                send(r);
            }
        }

    }

    private static void send(Reminder r) {
        r.waitingAck = true;
        TextChannel channel = Bot.jda.getTextChannelById(r.channelId);
        if (channel != null) {
            ((MessageCreateAction)((MessageCreateAction)channel.sendMessage("\ud83d\udd14 @everyone **" + r.title + "**").setAllowedMentions(EnumSet.of(MentionType.EVERYONE))).addActionRow(new ItemComponent[]{Button.success("done:" + r.id, "✔ Done")})).queue();
        }
    }

    static {
        scheduler.scheduleAtFixedRate(ReminderManager::tick, 0L, 5L, TimeUnit.SECONDS);
    }
}
