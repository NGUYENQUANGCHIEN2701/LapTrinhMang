package com.tankzors.client.html;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.google.gwt.user.client.Window;
import com.tankzors.client.GameClient;
import com.tankzors.client.gameplay.result.MatchResult;
import com.tankzors.client.session.LocalCampaignSession;
import com.tankzors.client.session.MapSource;

/** HTML5 entry point for the client-authoritative campaign. */
public class HtmlLauncher extends GwtApplication {

    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration config = new GwtApplicationConfiguration(800, 600);
        config.antialiasing = true;
        return config;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        String levelId = readParam("levelId", GameClient.DEFAULT_LEVEL);
        LocalCampaignSession session = new LocalCampaignSession(
                browserPlayerId(),
                levelId,
                new BootstrapMapSource(),
                (MatchResult result) -> {
                    GwtAppBridge.get().submitResult(result);
                    return true;
                });
        return new GameClient(session);
    }

    private static native String browserPlayerId() /*-{
        try {
            var value = $wnd.sessionStorage.getItem("tankzors_pid");
            return value ? "browser-" + value : "browser-anon";
        } catch(e) {
            return "browser-anon";
        }
    }-*/;

    private static String readParam(String name, String fallback) {
        try {
            String value = Window.Location.getParameter(name);
            if (value != null && !value.isEmpty()) return value;
        } catch (Exception ignored) {}
        return fallback;
    }

    static final class BootstrapMapSource implements MapSource {
        @Override public String loadMapJson(String levelId) { return null; }
        @Override public String newRunId(String levelId) { return "pending"; }
        @Override public String mapVersion(String levelId) { return ""; }

        @Override
        public void requestLevel(String levelId, LevelCallback callback) {
            fetchBootstrap("/api/game/bootstrap?levelId=" + encode(levelId) + "&mode=campaign", callback);
        }

        private native void fetchBootstrap(String url, LevelCallback callback) /*-{
            try {
                var xhr = new XMLHttpRequest();
                xhr.open("GET", url, true);
                xhr.onreadystatechange = $entry(function() {
                    if (xhr.readyState !== 4) return;
                    if (xhr.status === 200) {
                        try {
                            var resp = JSON.parse(xhr.responseText);
                            callback.@com.tankzors.client.session.MapSource$LevelCallback::onLevelLoaded(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(
                                resp.levelId,
                                JSON.stringify(resp.map),
                                resp.runId || ("local-" + resp.levelId),
                                resp.mapVersion || ""
                            );
                        } catch (e) {
                            callback.@com.tankzors.client.session.MapSource$LevelCallback::onLoadError(Ljava/lang/String;)("parse: " + e);
                        }
                    } else {
                        callback.@com.tankzors.client.session.MapSource$LevelCallback::onLoadError(Ljava/lang/String;)("HTTP " + xhr.status);
                    }
                });
                xhr.send();
            } catch (e) {
                callback.@com.tankzors.client.session.MapSource$LevelCallback::onLoadError(Ljava/lang/String;)("fetch: " + e);
            }
        }-*/;

        private static String encode(String value) { return jsEncode(value); }
        private static native String jsEncode(String value) /*-{
            return encodeURIComponent(value);
        }-*/;
    }
}
