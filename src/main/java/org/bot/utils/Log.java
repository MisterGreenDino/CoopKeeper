package org.bot.utils;

public final class Log {

    private static final String RESET = "\u001B[0m";

    // ANSI colors
    private static final String GRAY   = "\u001B[90m";
    private static final String BLUE   = "\u001B[34m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED    = "\u001B[31m";
    private static final String PURPLE = "\u001B[35m";

    private Log() {
        // Utility class
    }

    public static void trace(String message) {
        print("TRACE", GRAY, message);
    }

    public static void info(String message) {
        print("INFO", BLUE, message);
    }

    public static void warn(String message) {
        print("WARN", YELLOW, message);
    }

    public static void error(String message) {
        print("ERROR", RED, message);
    }

    public static void fatal(String message) {
        print("FATAL", PURPLE, message);
    }

    private static void print(String level, String color, String message) {
        System.out.println(
                color +
                        "[" + java.time.LocalTime.now().withNano(0) + "]" +
                        "(" + level + "): " +
                        message +
                        RESET
        );
    }
}