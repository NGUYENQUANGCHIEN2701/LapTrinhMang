package com.tankzors.client.html;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.google.gwt.user.client.Window;

import com.tankzors.client.GameClient;
import com.tankzors.client.audio.AudioManager;
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
        // Không truyền kích thước: canvas lấp đầy iframe và tự resize (kể cả fullscreen).
        GwtApplicationConfiguration config = new GwtApplicationConfiguration();
        // Mặc định LibGDX chừa 10px mỗi chiều, để lộ viền nền iframe quanh canvas.
        config.padHorizontal = 0;
        config.padVertical = 0;
        // Pixel art vẽ đúng lưới tile: MSAA không làm đẹp hơn mà tốn GPU mỗi frame.
        config.antialiasing = false;
        return config;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        String levelId = readParam("levelId", GameClient.DEFAULT_LEVEL);
        String mode = readParam("mode", "campaign");
        String wsUrl = readParam("wsUrl", defaultWsUrl());
        String soundVolStr = readParam("soundVol", "80");
        String musicVolStr = readParam("musicVol", "70");

        GameSocket socket = new BrowserWebSocket(wsUrl);
        GameClient client = new GameClient(wsUrl, levelId, socket);
        client.setBackToCampaignsHandler(() -> navigateTop("/campaigns"));

        float soundVol = parseVolume(soundVolStr, 0.8f);
        float musicVol = parseVolume(musicVolStr, 0.7f);
        client.setAudio(new AudioManager(soundVol, musicVol));

        if ("campaign".equalsIgnoreCase(mode)) {
            BootstrapMapSource mapSource = new BootstrapMapSource();
            // ResultHandler: kết quả VICTORY gửi REST (Bước D BrowserResultGateway).
            LocalCampaignSession session = new LocalCampaignSession(
                    browserPlayerId(), levelId, mapSource,
                    (MatchResult r) -> { GwtAppBridge.get().submitResult(r); return true; });
            session.setShopSyncHandler((coins, stars, researchJson, inventoryJson) -> {
                GwtAppBridge.get().syncShop(coins, stars, researchJson, inventoryJson);
            });
            client.setSession(session);
            // start() (fetch bootstrap ban đầu) được GameClient.create() gọi.
            // Restart/nextLevel sau này cũng qua cùng requestLevel path.
        }
        // else: legacy server-authoritative (chỉ tới khi Bước E tích hợp host/guest).

        return client;
    }

    /** Thoát hẳn khỏi iframe game để trở về trang chọn màn campaign. */
    private static native void navigateTop(String path) /*-{
        try {
            if ($wnd.top && $wnd.top !== $wnd) {
                $wnd.top.location.assign(path);
            } else {
                $wnd.location.assign(path);
            }
        } catch (e) {
            $wnd.location.assign(path);
        }
    }-*/;

    private static float parseVolume(String s, float def) {
        try {
            int v = Integer.parseInt(s);
            return Math.max(0f, Math.min(100f, (float) v)) / 100f;
        } catch (Exception ignored) {
            return def;
        }
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

    /**
     * Nguồn map qua REST bootstrap (Bước D). Tải <b>bất đồng bộ</b> qua XMLHttpRequest:
     * kết quả về qua {@link MapSource.LevelCallback} (JSNI callback quay lại Java),
     * không bao giờ trả sync — nên {@link #loadMapJson} luôn null.
     */
    static final class BootstrapMapSource implements MapSource {
        @Override public String loadMapJson(String levelId) { return null; } // async; trả null khi sync
        @Override public String newRunId(String levelId) { return "pending"; }
        @Override public String mapVersion(String levelId) { return ""; }

        @Override
        public void requestLevel(String levelId, LevelCallback callback) {
            String url = "/api/game/bootstrap?levelId=" + encode(levelId) + "&mode=campaign";
            fetchBootstrap(url, callback);
        }

        /** Fetch async; kích đúng 1 callback (onLevelLoaded hoặc onLoadError). */
        private native void fetchBootstrap(String url, LevelCallback callback) /*-{
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
                            var totalCoins = resp.totalCoins || 0;
                            var totalStars = resp.totalStars || 0;
                            var researchJson = resp.researchJson || "{}";
                            var inventoryJson = resp.inventoryJson || "{}";
                            callback.@com.tankzors.client.session.MapSource$LevelCallback::onLevelLoadedWithShop(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;)(levelId, mapJson, runId, mapVersion, totalCoins, totalStars, researchJson, inventoryJson);
                        } catch (e) {
                            $wnd.console && $wnd.console.error("[bootstrap] parse failed", e);
                            callback.@com.tankzors.client.session.MapSource$LevelCallback::onLoadError(Ljava/lang/String;)("parse: " + e);
                        }
                    } else {
                        $wnd.console && $wnd.console.error("[bootstrap] HTTP " + xhr.status);
                        callback.@com.tankzors.client.session.MapSource$LevelCallback::onLoadError(Ljava/lang/String;)("HTTP " + xhr.status);
                    }
                });
                xhr.send();
            } catch (e) {
                $wnd.console && $wnd.console.error("[bootstrap] request failed", e);
                callback.@com.tankzors.client.session.MapSource$LevelCallback::onLoadError(Ljava/lang/String;)("fetch: " + e);
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
