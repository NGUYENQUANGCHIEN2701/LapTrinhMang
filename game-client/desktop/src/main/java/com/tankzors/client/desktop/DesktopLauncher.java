package com.tankzors.client.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import com.tankzors.client.GameClient;
import com.tankzors.client.network.GameSocket;
import com.tankzors.client.session.LocalCampaignSession;

/**
 * LWJGL3 desktop launcher. Mặc định chạy campaign LOCAL (không cần server);
 * dùng {@code --online} để về chế độ server-authoritative cũ (debug).
 *
 * <p>Args: {@code --level campaign_1} {@code --online} {@code --ws ws://...}</p>
 */
public final class DesktopLauncher {

    public static void main(String[] args) {
        String wsUrl = GameClient.DEFAULT_WS_URL;
        String levelId = GameClient.DEFAULT_LEVEL;
        boolean online = false;

        for (int i = 0; i + 1 < args.length; i += 2) {
            if (args[i].equalsIgnoreCase("--ws")) wsUrl = args[i + 1];
            else if (args[i].equalsIgnoreCase("--level")) levelId = args[i + 1];
            else if (args[i].equalsIgnoreCase("--online") && args[i + 1].equalsIgnoreCase("true")) online = true;
        }
        if (!online) {
            // --online có thể đứng một mình
            for (String a : args) if (a.equalsIgnoreCase("--online")) online = true;
        }

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Tankzors - Desktop (local campaign)");
        config.setWindowedMode(800, 600);
        config.setForegroundFPS(60);
        config.useVsync(true);

        GameSocket socket = new DesktopWebSocket(wsUrl);
        GameClient client = new GameClient(wsUrl, levelId, socket);

        if (!online) {
            // Campaign local: MapSource load map qua requestLevel (sync trên desktop).
            DesktopMapSource mapSource = new DesktopMapSource();
            LocalCampaignSession session = new LocalCampaignSession(
                    "desktop-player", levelId, mapSource,
                    result -> { System.out.println("[Match] " + result.status + " score=" + result.score); return true; });
            client.setSession(session);
            // start() được GameClient.create() gọi (sau khi Gdx.files đã sẵn sàng).
        }

        try {
            new Lwjgl3Application(client, config);
        } catch (Exception e) {
            System.err.println("[DesktopLauncher] Lỗi khởi tạo LibGDX: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
