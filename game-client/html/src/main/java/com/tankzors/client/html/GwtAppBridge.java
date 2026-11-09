package com.tankzors.client.html;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.google.gwt.user.client.Timer;
import com.tankzors.client.gameplay.result.MatchResult;
import com.tankzors.client.session.CampaignPersistence;
import java.util.ArrayList;
import java.util.List;

/** Ordered, retryable outbox. Same-origin HTTP carries the authenticated session. */
public final class GwtAppBridge implements CampaignPersistence {
    private static final GwtAppBridge INSTANCE = new GwtAppBridge();
    public static GwtAppBridge get() { return INSTANCE; }
    public static void bootstrapRevision(String value) { INSTANCE.revision = Long.parseLong(value); }
    public interface Ready { void ready(); void failed(String reason); }
    private interface Response { void complete(int status, String response); }
    private static final Callback RECOVERED = new Callback() {
        public void saved(int coins, int stars, long revision) {}
        public void failed(String reason) {}
    };
    private static final class Request {
        String url, id, operations, body;
        Callback callback;
        int attempt;
    }
    private final List<Request> queue = new ArrayList<>();
    private final List<Ready> waiters = new ArrayList<>();
    private String playerId, error;
    private long revision;
    private boolean sending, preparing;
    private boolean reloadRequired;
    private GwtAppBridge() {}

    /** Recover this account's unfinished writes before starting another run. */
    public void prepare(Ready ready) {
        waiters.add(ready);
        if (playerId != null) { if (error != null) retry(); else pump(); return; }
        if (preparing) return;
        preparing = true;
        http("GET", "/api/game/account", null, (status, response) -> {
            preparing = false;
            if (status != 200) { failWaiters(message(status)); return; }
            try {
                JsonValue account = new JsonReader().parse(response);
                playerId = account.getString("playerId"); revision = account.getLong("revision");
                String stored = storage(playerId, null, false);
                if (stored != null && !stored.isEmpty()) {
                    JsonValue entries = new JsonReader().parse(stored);
                    for (JsonValue entry = entries.child; entry != null; entry = entry.next) {
                        Request r = new Request(); r.url = entry.getString("url"); r.id = entry.getString("id", "");
                        r.operations = entry.getString("operations", null); r.body = entry.getString("body", null);
                        r.callback = RECOVERED; queue.add(r);
                    }
                }
                pump();
            } catch (Exception e) { failWaiters("Không đọc được dữ liệu lưu. Vui lòng đăng nhập lại."); }
        });
    }
    @Override public void sync(String runId, String operations, Callback callback) {
        Request r = new Request(); r.url = "/api/game/shop/sync"; r.id = newId();
        r.operations = "\"runId\":" + quote(runId) + ",\"requestId\":" + quote(r.id) + ",\"operations\":" + operations;
        r.callback = callback; queue.add(r); remember(); pump();
    }
    @Override public void submit(MatchResult result, Callback callback) {
        Request r = new Request(); r.url = "/api/game/results"; r.id = result.runId;
        r.body = buildBody(result); r.callback = callback;
        queue.add(r); remember(); pump();
    }
    @Override public boolean isBusy() { return preparing || sending || !queue.isEmpty(); }
    @Override public void retry() {
        if (sending) return;
        if (reloadRequired) { reload(); return; }
        error = null;
        if (!queue.isEmpty()) queue.get(0).attempt = 0;
        pump();
    }
    private void pump() {
        if (sending || error != null || playerId == null) return;
        if (queue.isEmpty()) {
            remember();
            List<Ready> ready = new ArrayList<>(waiters); waiters.clear();
            for (Ready waiter : ready) waiter.ready();
            return;
        }
        Request r = queue.get(0);
        if (r.body == null) r.body = "{" + r.operations + ",\"expectedRevision\":" + revision + "}";
        remember(); sending = true;
        http("POST", r.url, r.body, (status, response) -> {
            sending = false;
            if (status >= 200 && status < 300) {
                try {
                    JsonValue json = new JsonReader().parse(response);
                    if (r.url.endsWith("results") && !json.getBoolean("saved", false)) throw new IllegalArgumentException();
                    revision = json.getLong("revision");
                    int coins = json.getInt("totalCoins"), stars = json.getInt("totalStars");
                    queue.remove(0); remember(); r.callback.saved(coins, stars, revision); pump();
                } catch (Exception e) { stop(r, "Máy chủ trả dữ liệu không hợp lệ. Vui lòng thử lại."); }
            } else if ((status == 0 || status >= 500 || status == 429) && r.attempt < 3) {
                int delay = 1000 * ++r.attempt;
                // Keep this request at the head; later writes cannot overtake its retry.
                sending = true;
                new Timer() { public void run() { sending = false; pump(); } }.schedule(delay);
            } else {
                if (status == 400 || status == 409) {
                    // Never rebase a rejected write. Preserve it for diagnosis and reload.
                    storage("rejected:" + playerId, storage(playerId, null, false), true);
                    reloadRequired = true;
                    for (Request pending : queue) if (pending != r) pending.callback.failed(message(status));
                    queue.clear(); remember();
                }
                stop(r, message(status));
            }
        });
    }
    private void stop(Request r, String reason) {
        error = reason; r.callback.failed(reason); failWaiters(reason);
    }
    private void failWaiters(String reason) {
        List<Ready> pending = new ArrayList<>(waiters); waiters.clear();
        for (Ready waiter : pending) waiter.failed(reason);
    }
    private void remember() {
        if (playerId == null) return;
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < queue.size(); i++) {
            Request r = queue.get(i); if (i > 0) out.append(',');
            out.append("{\"url\":").append(quote(r.url)).append(",\"id\":").append(quote(r.id));
            if (r.operations != null) out.append(",\"operations\":").append(quote(r.operations));
            if (r.body != null) out.append(",\"body\":").append(quote(r.body));
            out.append('}');
        }
        storage(playerId, out.append(']').toString(), true);
    }
    private static String message(int status) {
        if (status == 401 || status == 403) return "Phiên đăng nhập không còn hợp lệ. Hãy đăng nhập lại.";
        if (status == 409) return "Tiến trình đã thay đổi ở phiên khác. Nhấn R để tải lại dữ liệu đã lưu.";
        if (status == 400) return "Máy chủ từ chối giao dịch. Nhấn R để tải lại dữ liệu đã lưu.";
        return "Chưa kết nối được máy chủ. Tiến trình đang chờ lưu.";
    }
    private static String buildBody(MatchResult r) {
        return "{\"runId\":" + quote(r.runId) + ",\"mode\":" + quote(r.mode)
                + ",\"levelId\":" + quote(r.levelId) + ",\"mapVersion\":" + quote(r.mapVersion)
                + ",\"status\":" + quote(r.status) + ",\"score\":" + r.score + ",\"hpRemain\":" + r.hpRemain
                + ",\"aiKills\":" + r.aiKills + ",\"elapsedSeconds\":" + r.elapsedSeconds
                + ",\"starsEarned\":" + r.starsEarned + ",\"coinsEarned\":" + r.coinsEarned + "}";
    }
    static String quote(String text) {
        if (text == null) return "\"\"";
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 32) {
                String hex = Integer.toHexString(c); out.append("\\u");
                for (int j = hex.length(); j < 4; j++) out.append('0'); out.append(hex);
            } else out.append(c);
        }
        return out.append('"').toString();
    }
    private static native String newId() /*-{
        return $wnd.crypto && $wnd.crypto.randomUUID ? $wnd.crypto.randomUUID() :
            "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function(c) {
                var r = Math.random() * 16 | 0; return (c === "x" ? r : (r & 3 | 8)).toString(16);
            });
    }-*/;
    private static native void reload() /*-{ $wnd.location.reload(); }-*/;
    private static native String storage(String player, String value, boolean write) /*-{
        try {
            var key = "tankzors_outbox:" + player;
            if (write) { $wnd.sessionStorage.setItem(key, value); return value; }
            return $wnd.sessionStorage.getItem(key);
        } catch (e) { return null; }
    }-*/;
    private static native void http(String method, String url, String body, Response callback) /*-{
        var xhr, done = false;
        var finish = $entry(function(status, response) {
            if (done) return; done = true;
            callback.@com.tankzors.client.html.GwtAppBridge$Response::complete(ILjava/lang/String;)(status, response || "");
        });
        try {
            xhr = new XMLHttpRequest(); xhr.open(method, url, true); xhr.timeout = 15000;
            if (body !== null) xhr.setRequestHeader("Content-Type", "application/json");
            xhr.onreadystatechange = function() { if (xhr.readyState === 4) finish(xhr.status, xhr.responseText); };
            xhr.onerror = xhr.ontimeout = function() { finish(0, ""); };
            xhr.send(body);
        } catch (e) { finish(0, ""); }
    }-*/;
}
