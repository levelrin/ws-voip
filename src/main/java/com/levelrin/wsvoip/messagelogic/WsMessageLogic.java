package com.levelrin.wsvoip.messagelogic;

import com.google.gson.JsonObject;
import io.javalin.websocket.WsMessageContext;

/**
 * It's responsible for handling messages from clients in WebSocket.
 */
public interface WsMessageLogic {

    /**
     * Handle the WebSocket message.
     * @param context For sending messages back to the client.
     * @param message JSON.
     */
    void handle(final WsMessageContext context, final JsonObject message);

}
