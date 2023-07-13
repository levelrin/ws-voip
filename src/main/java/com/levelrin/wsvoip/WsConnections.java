package com.levelrin.wsvoip;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.javalin.websocket.WsContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WsConnections {

    /**
     * We must maintain its reverse (in a sense) map {@link WsConnections#sessionToUsername}.
     * We chose time complexity over space complexity.
     * Key - Username
     * Value - WebSocket context.
     */
    private final Map<String, WsContext> usernameToContext = new HashMap<>();

    /**
     * We must maintain its reverse (in a sense) map {@link WsConnections#usernameToContext}.
     * We chose time complexity over space complexity.
     * Key - WebSocket session ID.
     * Value - Username.
     */
    private final Map<String, String> sessionToUsername = new HashMap<>();

    /**
     * Thread lock.
     */
    private final Object lock = new Object();

    /**
     * Add a new user.
     * If the user exists already, it means the user is trying to connect to the server with another device.
     * In that case, we will replace the context with the new one.
     * @param username As is.
     * @param context New context.
     */
    public void add(final String username, final WsContext context) {
        synchronized (this.lock) {
            if (this.usernameToContext.containsKey(username)) {
                final WsContext oldContext = this.usernameToContext.get(username);
                this.usernameToContext.replace(username, context);
                this.sessionToUsername.remove(oldContext.getSessionId());
                this.sessionToUsername.put(context.getSessionId(), username);
                final JsonObject message = new JsonObject();
                message.addProperty("about", "another device is used");
                oldContext.send(message.toString());
            } else {
                this.usernameToContext.put(username, context);
                this.sessionToUsername.put(context.getSessionId(), username);
                final JsonObject message = new JsonObject();
                message.addProperty("about", "user is connected to the websocket server");
                message.addProperty("username", username);
                for (final WsContext each: this.usernameToContext.values()) {
                    each.send(message.toString());
                }
            }
        }
    }

    /**
     * This method can be called when the user tries to use multiple devices.
     * In other words, the old device will close the connection, while the new device will establish the connection.
     * In that case, the server won't broadcast the message that the user is disconnected.
     * @param sessionId As is.
     */
    public void remove(final String sessionId) {
        synchronized (this.lock) {
            if (this.sessionToUsername.containsKey(sessionId)) {
                final String username = this.sessionToUsername.get(sessionId);
                this.sessionToUsername.remove(sessionId);
                this.usernameToContext.remove(username);
                final JsonObject message = new JsonObject();
                message.addProperty("about", "user is disconnected from the websocket server");
                message.addProperty("username", username);
                for (final WsContext each: this.usernameToContext.values()) {
                    each.send(message.toString());
                }
            }
        }
    }

    public void broadcast(final JsonObject message) {
        synchronized (this.lock) {
            for (final WsContext context: this.usernameToContext.values()) {
                if (context.session.isOpen()) {
                    context.send(message.toString());
                }
            }
        }
    }

    public boolean hasUser(final String name) {
        synchronized (this.lock) {
            return this.usernameToContext.containsKey(name);
        }
    }

    /**
     * It's for the response body of '/onlineUsers' endpoint.
     * See details at doc/ws.md
     * @return Information of online users in JSON.
     */
    public JsonObject onlineUsers() {
        synchronized (this.lock) {
            final JsonObject result = new JsonObject();
            final JsonArray users = new JsonArray();
            for (final String username : this.usernameToContext.keySet()) {
                users.add(username);
            }
            result.add("users", users);
            return result;
        }
    }

    /**
     * Get username by the WebSocket session ID.
     * @param sessionId As is.
     * @return As is.
     */
    public String username(final String sessionId) {
        synchronized (this.lock) {
            return this.sessionToUsername.get(sessionId);
        }
    }

}
