package com.levelrin.wsvoip;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.levelrin.wsvoip.messagelogic.HandleArrayOfFloat32Array;
import com.levelrin.wsvoip.messagelogic.WsMessageLogic;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(final String... args) {
        final WsConnections wsConnections = new WsConnections();
        final VoiceChannels voiceChannels = new VoiceChannels();
        final PebbleEngine pebbleEngine = new PebbleEngine.Builder().build();
        final PebbleTemplate mainTemplate = pebbleEngine.getTemplate("template/main.html");
        final Map<String, WsMessageLogic> messageLogicMap = new HashMap<>();
        messageLogicMap.put("audio data", new HandleArrayOfFloat32Array());
        final Javalin app = Javalin
            .create(config -> {
                config.staticFiles.add("/public", Location.CLASSPATH);
            }).get("/yoi", context -> {
                context.result("Yoi Yoi\n");
            }).post("/main", context -> {
                if (context.formParamMap().containsKey("username")) {
                    context.contentType(ContentType.HTML);
                    Map<String, Object> pebbleContext = new HashMap<>();
                    pebbleContext.put("username", context.formParam("username"));
                    final Writer writer = new StringWriter();
                    mainTemplate.evaluate(writer, pebbleContext);
                    context.result(writer.toString());
                } else {
                    context.status(400);
                    context.result(
                        String.format(
                            "We expect that the content-type is application/x-www-form-urlencoded and the form parameter 'username' exists. Request body was: %s",
                            context.body()
                        )
                    );
                }
            }).get("/onlineUsers", context -> {
                context.status(200);
                context.result(wsConnections.onlineUsers().toString());
            }).post("/createVoiceChannel", context -> {
                if (contentTypeSpecified(context) && contentTypeIsPlainText(context)) {
                    final String channelName = context.body();
                    voiceChannels.create(channelName, context, wsConnections);
                }
            }).delete("/removeVoiceChannel", context -> {
                if (contentTypeSpecified(context) && contentTypeIsPlainText(context)) {
                    final String channelName = context.body();
                    voiceChannels.remove(channelName, context, wsConnections);
                }
            }).post("/joinVoiceChannel", context -> {
                if (
                    contentTypeSpecified(context)
                        && contentTypeIsJson(context)
                        && bodyIsJsonObject(context)
                        && jsonBodyHas("username", context)
                        && jsonBodyHas("channelName", context)
                ) {
                    final JsonObject body = new Gson().fromJson(context.body(), JsonObject.class);
                    voiceChannels.join(
                        body.get("username").getAsString(),
                        body.get("channelName").getAsString(),
                        context,
                        wsConnections
                    );
                }
            }).post("/leaveVoiceChannel", context -> {
                if (
                    contentTypeSpecified(context)
                        && contentTypeIsJson(context)
                        && bodyIsJsonObject(context)
                        && jsonBodyHas("username", context)
                        && jsonBodyHas("channelName", context)
                ) {
                    final JsonObject body = new Gson().fromJson(context.body(), JsonObject.class);
                    voiceChannels.leave(
                        body.get("username").getAsString(),
                        body.get("channelName").getAsString(),
                        context,
                        wsConnections
                    );
                }
            }).post("/switchVoiceChannel", context -> {
                if (
                    contentTypeSpecified(context)
                        && contentTypeIsJson(context)
                        && bodyIsJsonObject(context)
                        && jsonBodyHas("username", context)
                        && jsonBodyHas("oldChannelName", context)
                        && jsonBodyHas("newChannelName", context)
                ) {
                    final JsonObject body = new Gson().fromJson(context.body(), JsonObject.class);
                    voiceChannels.switchChannel(
                        body.get("username").getAsString(),
                        body.get("oldChannelName").getAsString(),
                        body.get("newChannelName").getAsString(),
                        context,
                        wsConnections
                    );
                }
            }).get("/voiceChannels", context -> {
                context.status(200);
                context.result(voiceChannels.json().toString());
            }).exception(Exception.class, (exception, context) -> {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(
                        String.format(
                            "Unexpected error occurred from the HTTP endpoint. URL: %s%n Headers: %s%n Body:%s%n",
                            context.fullUrl(),
                            context.headerMap(),
                            context.body()
                        ),
                        exception
                    );
                }
            });
        app.ws("/connect", ws -> {
            ws.onConnect(context -> {
                context.session.setIdleTimeout(Duration.ofDays(1));
                if (usernameExists(context)) {
                    final String username = URLDecoder.decode(
                        Objects.requireNonNull(context.queryParam("username")),
                        StandardCharsets.UTF_8
                    );
                    wsConnections.add(username, context);
                } else {
                    context.closeSession(
                        5000,
                        String.format(
                            "We expect 'username' query string for ws '/connect' request. Actual query string: %s",
                            context.queryString()
                        )
                    );
                }
            });
            ws.onClose(context -> {
                voiceChannels.leave(
                    wsConnections.username(context.getSessionId()),
                    wsConnections
                );
                wsConnections.remove(context.getSessionId());
                context.closeSession();
            });
            ws.onMessage(context -> {
                if (wsMessageIsJsonObject(context)) {
                    final JsonObject messageJson = new Gson().fromJson(
                        context.message(),
                        JsonObject.class
                    );
                    if (aboutAttributeExists(messageJson)) {
                        final String about = messageJson.get("about").getAsString();
                        if (messageLogicMap.containsKey(about)) {
                            messageLogicMap.get(about).handle(context, messageJson);
                        } else {
                            if (LOGGER.isWarnEnabled()) {
                                LOGGER.warn(
                                    String.format(
                                        "We got a suspicious/unsupported message from the WebSocket. Message: %s",
                                        context.message()
                                    )
                                );
                            }
                        }
                    }
                }
            });
            ws.onError(context -> {
                try {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error("Unexpected error occurred from the WebSocket.", context.error());
                    }
                    wsConnections.remove(context.getSessionId());
                    context.closeSession(
                        1011,
                        "Unexpected error occurred from the WebSocket. Please check the server log."
                    );
                } catch (final Exception exception) {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error("Failed to handle the error.", exception);
                    }
                }
            });
        });
        app.start();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Server is ready!");
        }
    }

    /**
     * Check if the 'Content-Type' header exists.
     * @param context It has the headers.
     * @return True if the 'Content-Type' header exists.
     */
    private static boolean contentTypeSpecified(final Context context) {
        boolean result = false;
        if (context.contentType() == null) {
            context.status(400);
            context.result("Content-Type header is missing.");
        } else {
            result = true;
        }
        return result;
    }

    /**
     * Check if the content type of the request body is in plain text.
     * @param context It has the headers.
     * @return True if the content type is in plain text.
     */
    private static boolean contentTypeIsPlainText(final Context context) {
        boolean result = false;
        final String contentType = Objects.requireNonNull(context.contentType()).toUpperCase(Locale.ROOT);
        if (contentType.contains("TEXT/PLAIN") && contentType.contains("CHARSET=UTF-8")) {
            result = true;
        } else {
            context.status(400);
            context.result(
                String.format(
                    "Content-Type should be 'text/plain; charset=utf-8', but it was: %s",
                    context.contentType()
                )
            );
        }
        return result;
    }

    /**
     * Check if the content type of the request body is in JSON.
     * @param context It has the headers.
     * @return True if the content type is in JSON.
     */
    private static boolean contentTypeIsJson(final Context context) {
        boolean result = false;
        final String contentType = Objects.requireNonNull(context.contentType()).toUpperCase(Locale.ROOT);
        if (contentType.contains("APPLICATION/JSON") && contentType.contains("CHARSET=UTF-8")) {
            result = true;
        } else {
            context.status(400);
            context.result(
                String.format(
                    "Content-Type should be 'application/json; charset=utf-8', but it was: %s",
                    context.contentType()
                )
            );
        }
        return result;
    }

    /**
     * Check if the request body is in valid JsonObject.
     * @param context It has the request body.
     * @return True if the request body is in valid JSON.
     */
    private static boolean bodyIsJsonObject(final Context context) {
        boolean valid = true;
        try {
            final JsonElement jsonElement = new Gson().fromJson(context.body(), JsonElement.class);
            if (!jsonElement.isJsonObject()) {
                valid = false;
            }
        } catch (final JsonSyntaxException | NullPointerException exception) {
            valid = false;
        }
        if (!valid) {
            context.status(400);
            final JsonObject body = new JsonObject();
            body.addProperty(
                "reason",
                String.format(
                    "The body is not in a valid JsonObject format. body: %s",
                    context.body()
                )
            );
            context.result(body.toString());
        }
        return valid;
    }

    /**
     * Check if the WebSocket message is in valid JsonObject.
     * @param context It has the message.
     * @return True if the message is in valid JSON.
     */
    private static boolean wsMessageIsJsonObject(final WsMessageContext context) {
        boolean valid = true;
        try {
            final JsonElement jsonElement = new Gson().fromJson(context.message(), JsonElement.class);
            if (!jsonElement.isJsonObject()) {
                valid = false;
            }
        } catch (final JsonSyntaxException | NullPointerException exception) {
            valid = false;
        }
        if (!valid) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(
                    String.format(
                        "WebSocket server received non-JSON message: %s",
                        context.message()
                    )
                );
            }
        }
        return valid;
    }

    /**
     * Check if the JSON object has the attribute 'about'.
     * @param json As is.
     * @return True if it has the attribute 'about'.
     */
    private static boolean aboutAttributeExists(final JsonObject json) {
        boolean valid = false;
        if (json.has("about")) {
            valid = true;
        } else {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(
                    String.format(
                        "We expect JSON message to have 'about' attribute, but it does not exist. JSON: %s",
                        json
                    )
                );
            }
        }
        return valid;
    }

    /**
     * Check if the request body (assuming it's in JSON) has the specified attribute.
     * @param attribute Check if this exists.
     * @param context It has the request body.
     * @return True if the JSON has the attribute.
     */
    private static boolean jsonBodyHas(final String attribute, final Context context) {
        return new Gson().fromJson(context.body(), JsonObject.class).has(attribute);
    }

    /**
     * Check the URL query string 'username' exists from the WebSocket connection request.
     * @param context It has the URL query string.
     * @return True is username existed.
     */
    private static boolean usernameExists(final WsContext context) {
        return context.queryParamMap().containsKey("username");
    }

}
