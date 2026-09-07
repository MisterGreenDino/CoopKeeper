//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.bot.reminder;

import java.time.Instant;

public class Reminder {
    public String id;
    public String name;
    public String title;
    public String channelId;
    public long intervalMs;
    public Instant nextTrigger;
    public boolean waitingAck = false;

    public Reminder() {
    }
}
