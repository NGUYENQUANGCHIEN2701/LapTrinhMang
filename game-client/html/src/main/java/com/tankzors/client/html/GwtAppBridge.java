package com.tankzors.client.html;

import com.google.gwt.core.client.GWT;
import com.tankzors.client.gameplay.result.MatchResult;

/**
 * Bridge từ Java sang trình duyệt cho REST: gửi kết quả trận (POST /api/game/results,
 * Bước D). Async qua XMLHttpRequest (JSNI). Idempotent: cùng runId + payload không
 * thay đổi ở server; retry 3 lần tự động rồi thôi.
 *
 * <p>Single instance qua {@link #get()}.</p>
 */
public final class GwtAppBridge {
    private static final GwtAppBridge INSTANCE = new GwtAppBridge();

    public static GwtAppBridge get() { return INSTANCE; }

    private GwtAppBridge() {}

    /** Gửi MatchResult lên server. Trả về true nếu cả 200 hoặc đã idempotent-match. */
    public void submitResult(MatchResult r) {
        if (r == null) return;
        String url = "/api/game/results";
        String body = buildBody(r);
        postJsonWithRetry(url, body, 0);
    }

    /** Đồng bộ dữ liệu shop (coins, stars, research, inventory) lên server. */
    public void syncShop(int coins, int stars, String researchJson, String inventoryJson) {
        String url = "/api/game/shop/sync";
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"totalCoins\":").append(coins).append(',');
        sb.append("\"totalStars\":").append(stars).append(',');
        sb.append("\"researchJson\":").append(quote(researchJson)).append(',');
        sb.append("\"inventoryJson\":").append(quote(inventoryJson));
        sb.append('}');
        postJsonWithRetry(url, sb.toString(), 0);
    }

    private String buildBody(MatchResult r) {
        // Hand-roll JSON (tránh String.format — GWT không emulate; tránh Gson — core).
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"runId\":").append(quote(r.runId)).append(',');
        sb.append("\"mode\":").append(quote(r.mode)).append(',');
        sb.append("\"levelId\":").append(quote(r.levelId)).append(',');
        sb.append("\"mapVersion\":").append(quote(r.mapVersion)).append(',');
        sb.append("\"status\":").append(quote(r.status)).append(',');
        sb.append("\"score\":").append(r.score).append(',');
        sb.append("\"hpRemain\":").append(r.hpRemain).append(',');
        sb.append("\"aiKills\":").append(r.aiKills).append(',');
        sb.append("\"elapsedSeconds\":").append(r.elapsedSeconds).append(',');
        sb.append("\"starsEarned\":").append(r.starsEarned).append(',');
        sb.append("\"coinsEarned\":").append(r.coinsEarned);
        sb.append('}');
        return sb.toString();
    }

    private static String quote(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.valueOf((char) 0)); // control: bỏ qua
                    else sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static final int MAX_RETRIES = 3;

    private native void postJsonWithRetry(String url, String body, int attempt) /*-{
        var self = this;
        try {
            var xhr = new XMLHttpRequest();
            xhr.open("POST", url, true);
            xhr.setRequestHeader("Content-Type", "application/json");
            xhr.onreadystatechange = $entry(function() {
                if (xhr.readyState !== 4) return;
                var ok = (xhr.status >= 200 && xhr.status < 300) || xhr.status === 409;
                if (!ok && attempt < 3) {
                    $wnd.setTimeout(function() {
                        self.@com.tankzors.client.html.GwtAppBridge::postJsonWithRetry(Ljava/lang/String;Ljava/lang/String;I)(url, body, attempt + 1);
                    }, 1000 * (attempt + 1));
                } else if (!ok) {
                    $wnd.console && $wnd.console.error("[results] HTTP " + xhr.status + " after retries");
                }
            });
            xhr.send(body);
        } catch (e) {
            $wnd.console && $wnd.console.error("[results] request failed", e);
        }
    }-*/;
}
