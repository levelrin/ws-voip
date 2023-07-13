package com.levelrin.wsvoip.messagelogic;

import com.google.gson.JsonObject;
import io.javalin.websocket.WsMessageContext;

public final class HandleArrayOfFloat32Array implements WsMessageLogic {

    @Override
    public void handle(final WsMessageContext context, final JsonObject message) {
        // todo: Do not send audio data to the source client.
        context.send(message.toString());
    }

}
