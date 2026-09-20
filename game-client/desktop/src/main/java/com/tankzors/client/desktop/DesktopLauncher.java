package com.tankzors.client.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.tankzors.client.GameClient;
import com.tankzors.client.session.LocalCampaignSession;

/** Desktop launcher for offline client-authoritative campaign development. */
public final class DesktopLauncher {

    private DesktopLauncher() {}

    public static void main(String[] args) {
        String levelId = GameClient.DEFAULT_LEVEL;
        for (int i = 0; i < args.length; i++) {
            if ("--level".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                levelId = args[++i];
            }
        }

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Tankzors - Desktop campaign");
        config.setWindowedMode(800, 600);
        config.setForegroundFPS(60);
        config.useVsync(true);

        DesktopMapSource mapSource = new DesktopMapSource();
        LocalCampaignSession session = new LocalCampaignSession(
                "desktop-player",
                levelId,
                mapSource,
                result -> {
                    System.out.println("[Match] " + result.status + " score=" + result.score);
                    return true;
                });
        new Lwjgl3Application(new GameClient(session), config);
    }
}
