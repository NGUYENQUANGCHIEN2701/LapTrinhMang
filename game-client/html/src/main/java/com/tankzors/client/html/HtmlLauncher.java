package com.tankzors.client.html;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.google.gwt.user.client.Window;

import com.tankzors.client.GameClient;
import com.tankzors.client.network.GameSocket;

/**
 * GWT entry point cho Tankzors HTML5 client.
 *
 * <p>Host page: {@code webapp/index.html} (sau khi nhúng, có URL
 * {@code /game/libgdx/index.html?levelId=campaign_2}).</p>
 *
 * <p>Level động: Thymeleaf page {@code play.html} nhúng iframe với
 * query string {@code levelId}; HtmlLauncher đọc qua
 * {@link Window.Location#getParameter}.</p>
 *
 * <p>WebSocket endpoint: host page và WebSocket cùng origin
 * ({@code ws://localhost:8081/game}); derives từ {@link Window.Location}.
 * Override qua {@code wsUrl} nếu cần.</p>
 */
public class HtmlLauncher extends GwtApplication {

    @Override
    public GwtApplicationConfiguration getConfig() {
        // LibGDX GWT sẽ chèn canvas vào body; 800x600 là kích thước logic.
        GwtApplicationConfiguration config = new GwtApplicationConfiguration(800, 600);
        config.antialiasing = true;
        return config;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        String levelId = readParam("levelId", GameClient.DEFAULT_LEVEL);
        String wsUrl = readParam("wsUrl", defaultWsUrl());
        GameSocket socket = new BrowserWebSocket(wsUrl);
        return new GameClient(wsUrl, levelId, socket);
    }

    private static String readParam(String name, String fallback) {
        try {
            String value = Window.Location.getParameter(name);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    /**
     * Derives WebSocket URL từ host page location: same host/port + path {@code /game}.
     * Vd page {@code http://localhost:8081/play} -> {@code ws://localhost:8081/game}.
     * HTTPS host -> {@code wss://}.
     */
    private static String defaultWsUrl() {
        String protocol = Window.Location.getProtocol();
        String host = Window.Location.getHost();
        String scheme = "https:".equalsIgnoreCase(protocol) ? "wss" : "ws";
        return scheme + "://" + host + "/game";
    }
}
