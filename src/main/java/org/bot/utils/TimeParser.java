package org.bot.utils;

public class TimeParser {
    public TimeParser() {
    }

    public static long parse(String input) {
        input = input.trim().toLowerCase();
        if (input.endsWith("h")) {
            return Long.parseLong(input.replace("h", "")) * 3600000L;
        } else if (input.endsWith("m")) {
            return Long.parseLong(input.replace("m", "")) * 60000L;
        } else if (input.endsWith("s")) {
            return Long.parseLong(input.replace("s", "")) * 1000L;
        } else {
            throw new IllegalArgumentException("Invalid time format");
        }
    }

    public static String format(long millis) {
        long seconds = millis / 1000L;
        long hours = seconds / 3600L;
        long minutes = seconds % 3600L / 60L;
        long secs = seconds % 60L;
        if (hours > 0L) {
            return hours + "h " + minutes + "m " + secs + "s";
        } else {
            return minutes > 0L ? minutes + "m " + secs + "s" : secs + "s";
        }
    }
}
