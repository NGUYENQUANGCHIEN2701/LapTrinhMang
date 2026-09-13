package com.tankzors.client.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import com.tankzors.client.GameClient;

/**
 * LWJGL3 desktop launcher cho Tankzors game-client.
 *
 * <p>Cửa sổ 800x600 (parity với canvas React). Vsync on, limit FPS 60.</p>
 *
 * <p>Thanh sang chạy: {@code java -jar tankzors-game-client-1.0-SNAPSHOT.jar}
 * hoặc pass args để override wsUrl/levelId.</p>
 */
public final class DesktopLauncher {

    public static void main(String[] args) {
        String wsUrl = GameClient.DEFAULT_WS_URL;
        String levelId = GameClient.DEFAULT_LEVEL;

        for (int i = 0; i + 1 < args.length; i += 2) {
            if (args[i].equalsIgnoreCase("--ws")) wsUrl = args[i + 1];
            else if (args[i].equalsIgnoreCase("--level")) levelId = args[i + 1];
        }

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Tankzors Pro - Desktop");
        config.setWindowedMode(800, 600);
        config.setForegroundFPS(60);
        config.useVsync(true);

        try {
            new Lwjgl3Application(new GameClient(wsUrl, levelId), config);
        } catch (Exception e) {
            System.err.println("[DesktopLauncher] Lỗi khởi tạo LibGDX: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
