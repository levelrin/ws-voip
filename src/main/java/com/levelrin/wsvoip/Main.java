package com.levelrin.wsvoip;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

public final class Main {

    public static void main(final String... args) {
        final Javalin app = Javalin
            .create(config -> {
                config.staticFiles.add("/public", Location.CLASSPATH);
            })
            .get("/yoi", context -> {
                context.result("Yoi Yoi\n");
            });
        app.start();
    }

}
