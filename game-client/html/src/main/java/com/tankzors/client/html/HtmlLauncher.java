package com.tankzors.client.html;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.google.gwt.user.client.Window;

import com.tankzors.client.GameClient;
import com.tankzors.client.network.GameSocket;
import com.tankzors.client.session.LocalCampaignSession;
import com.tankzors.client.session.MapSource;
import com.tankzors.client.gameplay.result.MatchResult;

/**
 * GWT entry point. Mặc định mode=campaign (local, không WebSocket): nạp map
 * qua {@code GET /api/game/bootstrap?levelId=...} (Bước D), rồi chạy simulation
 * 100% client. Khi server không có endpoint / bootstrap lỗi, session ở WAITING
 * và màn hình loading hiện lỗi (plan mục 5).
 *
 * <p>{@code --mode} param: {@code campaign} (mặc định) hoặc {@code legacy}
 * (server-authoritative cũ, debug tới khi Bước E hoàn tất).</p>
 */
public class HtmlLauncher extends GwtApplication {

    private static final String TAG = "HtmlLauncher";

    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration config = new GwtApplicationConfiguration(800, 600);
        config.antialiasing = true;
        return config;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        String levelId = readParam("levelId", GameClient.DEFAULT_LEVEL);
        String mode = readParam("mode", "campaign");
        String wsUrl = readParam("wsUrl", defaultWsUrl());

        GameSocket socket = new BrowserWebSocket(wsUrl);
        GameClient client = new GameClient(wsUrl, levelId, socket);

        if ("campaign".equalsIgnoreCase(mode)) {
            BootstrapMapSource mapSource = new BootstrapMapSource();
            // ResultHandler: kết quả VICTORY gửi REST (Bước D BrowserResultGateway).
            LocalCampaignSession session = new LocalCampaignSession(
                    browserPlayerId(), levelId, mapSource,
                    (MatchResult r) -> { GwtAppBridge.get().submitResult(r); return true; });
            client.setSession(session);
            // Kích hoạt fetch bootstrap (async JSNI callback quay lại Java).
            mapSource.request(levelId, session);
        }
        // else: legacy server-authoritative (chỉ tới khi Bước E tích hợp host/guest).

        return client;
    }

    /** Player identity tạm ngữ phi đỡnigotに trình duyệt — Bước D resolve từ HTTP session. */
    private static native String browserPlayerId() /*-{
        try { return "browser-" + $wnd.sessionStorage.getItem("tankzors_pid"); }
        catch(e) { return "browser-anon"; }
    }-*/;

    private static String readParam(String name, String fallback) {
        try {
            String value = Window.Location.getParameter(name);
            if (value != null && !value.isEmpty()) return value;
        } catch (Exception ignored) {}
        return fallback;
    }

    private static String defaultWsUrl() {
        String protocol = Window.Location.getProtocol();
        String host = Window.Location.getHost();
        String scheme = "https:".equalsIgnoreCase(protocol) ? "wss" : "ws";
        return scheme + "://" + host + "/game";
    }

    // ===== JSNI bridge cho REST bootstrap/result (async) =====

    /** Nguồn map qua REST bootstrap (Bước D). Load async qua XMLHttpRequest. */
    static final class BootstrapMapSource implements MapSource {
        @Override public String loadMapJson(String levelId) { return null; } // async; trả null khi sync
        @Override public String newRunId(String levelId) { return "pending"; }
        @Override public String mapVersion(String levelId) { return ""; }

        void request(String levelId, LocalCampaignSession session) {
            String url = "/api/game/bootstrap?levelId=" + encode(levelId) + "&mode=campaign";
            fetchBootstrap(url, session);
        }

        private native void fetchBootstrap(String url, LocalCampaignSession session) /*-{
            var self = this;
            try {
                var xhr = new XMLHttpRequest();
                xhr.open("GET", url, true);
                xhr.onreadystatechange = $entry(function() {
                    if (xhr.readyState !== 4) return;
                    if (xhr.status === 200) {
                        try {
                            var resp = JSON.parse(xhr.responseText);
                            var mapJson = JSON.stringify(resp.map);
                            var runId = resp.runId || ("local-" + resp.levelId);
                            var mapVersion = resp.mapVersion || "";
                            var levelId = resp.levelId;
                            session.@com.tankzors.client.session.LocalCampaignSession::changeLevel(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(levelId, mapJson, runId, mapVersion);
                        } catch (e) {
                            $wnd.console && $wnd.console.error("[bootstrap] parse failed", e);
                        }
                    } else {
                        $wnd.console && $wnd.console.error("[bootstrap] HTTP " + xhr.status);
                    }
                });
                xhr.send();
            } catch (e) {
                $wnd.console && $wnd.console.error("[bootstrap] request failed", e);
            }
        }-*/;

        private static String encode(String s) {
            return jsEncode(s);
        }
        private static native String jsEncode(String s) /*-{
            return encodeURIComponent(s);
        }-*/;
    }
}
