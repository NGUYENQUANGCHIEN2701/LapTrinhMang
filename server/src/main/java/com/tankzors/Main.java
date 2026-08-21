package com.tankzors;

import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
        });

        app.get("/", ctx -> ctx.result("Tankzors Server is running"));

        app.start(ServerConfig.PORT);

        System.out.println("Tankzors Server started on port " + ServerConfig.PORT);
    }
}
