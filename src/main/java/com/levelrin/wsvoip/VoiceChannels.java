package com.levelrin.wsvoip;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VoiceChannels {

    /**
     * We must maintain its reverse (in a sense) map {@link VoiceChannels#usernameToChannel}.
     * We chose time complexity over space complexity.
     * Key - Channel name.
     * Value - Usernames in that channel.
     */
    private final Map<String, List<String>> channelToUsernames = new HashMap<>();

    /**
     * We must maintain its reverse (in a sense) map {@link VoiceChannels#channelToUsernames}.
     * We chose time complexity over space complexity.
     * Key - Username.
     * Value - User's channel name.
     */
    private final Map<String, String> usernameToChannel = new HashMap<>();

    /**
     * Thread lock.
     */
    private final Object lock = new Object();

    /**
     * Create a new channel.
     * @param name Channel name.
     * @param httpContext To configure the HTTP response.
     * @param wsConnections To broadcast the event of channel creation.
     */
    public void create(final String name, final Context httpContext, final WsConnections wsConnections) {
        int statusCode;
        synchronized (this.lock) {
            if (this.channelToUsernames.containsKey(name)) {
                statusCode = 409;
            } else {
                statusCode = 201;
                this.channelToUsernames.put(name, new ArrayList<>());
            }
        }
        if (statusCode == 409) {
            httpContext.status(409);
            httpContext.header("Content-Type", "application/json");
            final JsonObject body = new JsonObject();
            body.addProperty("reason", "The channel exists already.");
            httpContext.result(body.toString());
        } else {
            httpContext.status(201);
            final JsonObject message = new JsonObject();
            message.addProperty("about", "voice channel is created");
            message.addProperty("name", name);
            wsConnections.broadcast(message);
        }
    }

    /**
     * Remove the channel.
     * @param name Channel name.
     * @param httpContext To configure the HTTP response.
     * @param wsConnections To broadcast the event of channel removal.
     */
    public void remove(final String name, final Context httpContext, final WsConnections wsConnections) {
        int statusCode;
        synchronized (this.lock) {
            if (this.channelToUsernames.containsKey(name)) {
                statusCode = 204;
                for (final String username: this.channelToUsernames.get(name)) {
                    this.usernameToChannel.remove(username);
                }
                this.channelToUsernames.remove(name);
            } else {
                statusCode = 404;
            }
        }
        if (statusCode == 204) {
            httpContext.status(204);
            final JsonObject message = new JsonObject();
            message.addProperty("about", "voice channel is removed");
            message.addProperty("name", name);
            wsConnections.broadcast(message);
        } else {
            httpContext.status(404);
            httpContext.header("Content-Type", "application/json");
            final JsonObject body = new JsonObject();
            body.addProperty("reason", "The channel does not exist.");
            httpContext.result(body.toString());
        }
    }

    /**
     * User joins the channel.
     * @param username AS is.
     * @param channelName As is.
     * @param httpContext To configure the HTTP response.
     * @param wsConnections To broadcast the event of joining.
     */
    public void join(final String username, final String channelName, final Context httpContext, final WsConnections wsConnections) {
        String status = "success";
        synchronized (this.lock) {
            if (wsConnections.hasUser(username)) {
                if (this.channelToUsernames.containsKey(channelName)) {
                    this.usernameToChannel.put(username, channelName);
                    this.channelToUsernames.get(channelName).add(username);
                } else {
                    status = "channel not found";
                }
            } else {
                status = "user not found";
            }
        }
        if ("success".equals(status)) {
            httpContext.status(204);
            final JsonObject message = new JsonObject();
            message.addProperty("about", "user joined the voice channel");
            message.addProperty("username", username);
            message.addProperty("channelName", channelName);
            wsConnections.broadcast(message);
        } else if ("user not found".equals(status)) {
            httpContext.status(404);
            httpContext.header("Content-Type", "application/json");
            final JsonObject body = new JsonObject();
            body.addProperty("reason", "The user does not exist.");
            httpContext.result(body.toString());
        } else {
            httpContext.status(404);
            httpContext.header("Content-Type", "application/json");
            final JsonObject body = new JsonObject();
            body.addProperty("reason", "The channel does not exist.");
            httpContext.result(body.toString());
        }
    }

    /**
     * User leaves the channel.
     * @param username As is.
     * @param channelName As is.
     * @param httpContext To configure the HTTP response.
     * @param wsConnections To broadcast the event of leaving.
     */
    public void leave(final String username, final String channelName, final Context httpContext, final WsConnections wsConnections) {
        String status = "success";
        synchronized (this.lock) {
            if (wsConnections.hasUser(username)) {
                if (this.channelToUsernames.containsKey(channelName)) {
                    this.usernameToChannel.remove(username);
                    this.channelToUsernames.get(channelName).remove(username);
                } else {
                    status = "channel not found";
                }
            } else {
                status = "user not found";
            }
        }
        if ("success".equals(status)) {
            httpContext.status(204);
            final JsonObject message = new JsonObject();
            message.addProperty("about", "user left the voice channel");
            message.addProperty("username", username);
            message.addProperty("channelName", channelName);
            wsConnections.broadcast(message);
        } else if ("user not found".equals(status)) {
            httpContext.status(404);
            httpContext.header("Content-Type", "application/json");
            final JsonObject body = new JsonObject();
            body.addProperty("reason", "The user does not exist.");
            httpContext.result(body.toString());
        } else {
            httpContext.status(404);
            httpContext.header("Content-Type", "application/json");
            final JsonObject body = new JsonObject();
            body.addProperty("reason", "The channel does not exist.");
            httpContext.result(body.toString());
        }
    }

    /**
     * User leaves the channel without API call.
     * For example, this method can be used when the user lost the WebSocket connection.
     * @param username As is.
     * @param wsConnections As is.
     */
    public void leave(final String username, final WsConnections wsConnections) {
        boolean success = false;
        String channelName = "TBD.";
        synchronized (this.lock) {
            if (wsConnections.hasUser(username)) {
                channelName = this.usernameToChannel.get(username);
                if (this.channelToUsernames.containsKey(channelName)) {
                    this.usernameToChannel.remove(username);
                    this.channelToUsernames.get(channelName).remove(username);
                    success = true;
                }
            }
        }
        if (success) {
            final JsonObject message = new JsonObject();
            message.addProperty("about", "user left the voice channel");
            message.addProperty("username", username);
            message.addProperty("channelName", channelName);
            wsConnections.broadcast(message);
        }
    }

    /**
     * User switches the channel.
     * @param username As is.
     * @param oldChannelName As is.
     * @param newChannelName As is.
     * @param httpContext To configure the HTTP response.
     * @param wsConnections To broadcast the event of switching the channel.
     */
    public void switchChannel(final String username, final String oldChannelName, final String newChannelName, final Context httpContext, final WsConnections wsConnections) {
        String status = "success";
        synchronized (this.lock) {
            if (wsConnections.hasUser(username)) {
                if (this.channelToUsernames.containsKey(oldChannelName)) {
                    if (this.channelToUsernames.containsKey(newChannelName)) {
                        if (this.usernameToChannel.containsKey(username)) {
                            if (this.usernameToChannel.get(username).equals(oldChannelName)) {
                                this.usernameToChannel.remove(username);
                                this.channelToUsernames.get(oldChannelName).remove(username);
                                this.usernameToChannel.put(username, newChannelName);
                                this.channelToUsernames.get(newChannelName).add(username);
                            } else {
                                status = "user not in old channel";
                                httpContext.status(400);
                                httpContext.header("Content-Type", "application/json");
                                final JsonObject body = new JsonObject();
                                body.addProperty(
                                    "reason",
                                    String.format(
                                        "The user was not in the old channel. The user was in %s.",
                                        this.usernameToChannel.get(username)
                                    )
                                );
                                httpContext.result(body.toString());
                            }
                        } else {
                            status = "user not in any channel";
                        }
                    } else {
                        status = "new channel not found";
                    }
                } else {
                    status = "old channel not found";
                }
            } else {
                status = "user not found";
            }
        }
        if ("success".equals(status)) {
            httpContext.status(204);
            final JsonObject message = new JsonObject();
            message.addProperty("about", "user switched the voice channel");
            message.addProperty("username", username);
            message.addProperty("oldChannelName", oldChannelName);
            message.addProperty("newChannelName", newChannelName);
            wsConnections.broadcast(message);
        } else if ("user not found".equals(status)) {
            httpContext.status(404);
            httpContext.header("Content-Type", "application/json");
            final JsonObject body = new JsonObject();
            body.addProperty("reason", "The user does not exist.");
            httpContext.result(body.toString());
        } else if ("old channel not found".equals(status)) {
            httpContext.status(404);
            httpContext.header("Content-Type", "application/json");
            final JsonObject body = new JsonObject();
            body.addProperty("reason", "The old channel does not exist.");
            httpContext.result(body.toString());
        } else if ("new channel not found".equals(status)) {
            httpContext.status(404);
            httpContext.header("Content-Type", "application/json");
            final JsonObject body = new JsonObject();
            body.addProperty("reason", "The new channel does not exist.");
            httpContext.result(body.toString());
        } else if ("user not in any channel".equals(status)) {
            httpContext.status(400);
            httpContext.header("Content-Type", "application/json");
            final JsonObject body = new JsonObject();
            body.addProperty("reason", "The user was not in any channel.");
            httpContext.result(body.toString());
        } else {
            // Already handled above.
        }
    }

    /**
     * It's for the response body of '/voiceChannels' endpoint.
     * See details at doc/voice-channels.md
     * @return Information of this object in JSON.
     */
    public JsonObject json() {
        synchronized (this.lock) {
            final JsonObject result = new JsonObject();
            final JsonArray channels = new JsonArray();
            for (final Map.Entry<String, List<String>> channelEntry : this.channelToUsernames.entrySet()) {
                final JsonArray users = new JsonArray();
                for (final String username : channelEntry.getValue()) {
                    users.add(username);
                }
                final JsonObject channel = new JsonObject();
                channel.addProperty("name", channelEntry.getKey());
                channel.add("users", users);
                channels.add(channel);
            }
            result.add("channels", channels);
            return result;
        }
    }

}
