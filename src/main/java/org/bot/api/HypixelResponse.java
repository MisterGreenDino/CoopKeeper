package org.bot.api;

import com.google.gson.JsonObject;

/**
 * Generic wrapper around a raw Hypixel API JSON response.
 */
public class HypixelResponse {

    private final boolean success;
    private final String cause;
    private final JsonObject data;

    public HypixelResponse(boolean success, String cause, JsonObject data) {
        this.success = success;
        this.cause = cause;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    /** Error reason reported by the API, or the exception message on transport failure. Null on success. */
    public String getCause() {
        return cause;
    }

    /** Raw JSON body. May be null if the request failed before a response was received. */
    public JsonObject getData() {
        return data;
    }
}
