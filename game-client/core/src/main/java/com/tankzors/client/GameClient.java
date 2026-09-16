package com.tankzors.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.tankzors.client.assets.AssetRepository;
import com.tankzors.client.input.ClientInputProcessor;
import com.tankzors.client.input.ClientInputProcessor.Direction;
import com.tankzors.client.model.GameStateSnapshot;
import com.tankzors.client.network.GameSocket;
import com.tankzors.client.network.PacketBuilder;
import com.tankzors.client.network.StatePacketParser;
import com.tankzors.client.render.GameRenderer;
import com.tankzors.client.session.GameSession;

/**
 * LibGDX entry point. Điều phối asset + render + input; delegate gameplay cho
 * {@link GameSession} (LocalCampaign / Host / Guest) hoặc fallback server-authoritative
 * (khi chưa có session — dùng trực tiếp socket).
 *
 * <p>Mode được chọn lúc create(): nếu có session → session-driven; nếu không → legacy
 * socket-driven (giữ tương thích ngược cho tới khi C/F hoàn tất).</p>
 */
public class GameClient implements ApplicationListener {

    private static final String TAG = "GameClient";

    public static final String DEFAULT_WS_URL = "ws://localhost:8081/game";
    public static final String DEFAULT_LEVEL = "campaign_1";

    private static final float PING_INTERVAL_SEC = 10f;

    private final String wsUrl;
    private String levelId;
    private final GameSocket socket;

    // Optional: session cung cấp gameplay local/relay (Bước C+). null = legacy.
    private GameSession session;

    private AssetRepository assets;
    private GameRenderer renderer;
    private StatePacketParser parser;
    private ClientInputProcessor input;

    // Legacy state (chỉ dùng khi session == null)
    private GameStateSnapshot legacyState;
    private boolean joinSent;
    private float pingTimer;
    private long lastPingSentMs;

    /** Constructor cũ: server-authoritative fallback (không session). */
    public GameClient(String wsUrl, String levelId, GameSocket socket) {
        this.wsUrl = wsUrl;
        this.levelId = levelId;
        this.socket = socket;
    }

    public GameSocket getSocket() { return socket; }

    public void setSession(GameSession session) {
        this.session = session;
    }

    public String getLevelId() { return levelId; }
    public void setLevelId(String levelId) { this.levelId = levelId; }

    @Override
    public void create() {
        Gdx.app.log(TAG, "Boot - wsUrl=" + wsUrl + " level=" + levelId + " session=" + (session != null));

        assets = new AssetRepository();
        assets.loadAll();

        renderer = new GameRenderer(assets);
        renderer.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        parser = new StatePacketParser();

        if (session != null && !session.requiresSocket()) {
            // Campaign local: KHÔNG connect WebSocket.
            Gdx.app.log(TAG, "Local session mode — no socket");
        } else if (session != null && session.requiresSocket()) {
            // Host/Guest: vẫn cần socket (Bước E).
            socket.connect();
        } else {
            // Legacy server-authoritative.
            socket.connect();
        }

        input = new ClientInputProcessor(new ClientInputProcessor.Callbacks() {
            @Override public void onMove(Direction direction, boolean moving) {
                if (session != null) session.onMove(direction, moving);
                else socket.send(PacketBuilder.moveInput(direction.name(), moving));
            }
            @Override public void onShoot() {
                if (session != null) session.onShoot();
                else socket.send(PacketBuilder.shootInput());
            }
            @Override public void onAllReleased() {
                if (session != null) session.onAllReleased();
                else socket.send(PacketBuilder.stop());
            }
            @Override public void onRestart() {
                if (session != null) session.restart();
                else restartLevel();
            }
            @Override public void onNextLevel() {
                if (session != null) session.nextLevel();
                else nextLevel();
            }
        });
        Gdx.input.setInputProcessor(input);
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();

        if (session != null) {
            // ===== Session-driven (local / host / guest) =====
            session.update(dt);
            setEnabledByStatus(session.status());
            if (renderer != null) renderer.render(session.snapshot());
            return;
        }

        // ===== Legacy server-authoritative =====
        String raw;
        while ((raw = socket.poll()) != null) handleIncoming(raw);

        if (socket.isConnected() && !joinSent) {
            socket.send(PacketBuilder.joinGameCampaign(levelId));
            joinSent = true;
        }

        if (socket.isConnected()) {
            pingTimer += dt;
            if (pingTimer >= PING_INTERVAL_SEC) {
                pingTimer = 0f;
                lastPingSentMs = System.currentTimeMillis();
                socket.send(PacketBuilder.ping());
            }
        }

        if (renderer != null) renderer.render(legacyState);
    }

    private void setEnabledByStatus(String status) {
        if (input == null) return;
        boolean playable = "PLAYING".equalsIgnoreCase(status);
        if (playable != input.isEnabled()) input.setEnabled(playable);
    }

    private void handleIncoming(String raw) {
        String type = parser.parseType(raw);
        if (type == null) return;
        if ("STATE".equals(type)) {
            try {
                GameStateSnapshot parsed = parser.parseState(raw);
                this.legacyState = parsed;
                if (input != null) {
                    boolean playable = "PLAYING".equalsIgnoreCase(parsed.gameStatus);
                    input.setEnabled(playable);
                }
            } catch (Exception e) {
                Gdx.app.error(TAG, "Parse STATE loi: " + e.getMessage());
            }
        } else if ("CONNECT".equals(type)) {
            String sessionId = parser.parseSessionId(raw);
            Gdx.app.log(TAG, "CONNECT sessionId=" + sessionId);
        } else if ("PONG".equals(type)) {
            long now = System.currentTimeMillis();
            long rtt = lastPingSentMs > 0 ? (now - lastPingSentMs) : -1;
            Gdx.app.debug(TAG, "PONG rtt=" + rtt + "ms");
        } else if ("ACK".equals(type)) {
            Gdx.app.debug(TAG, "ACK");
        } else if ("ERROR".equals(type)) {
            Gdx.app.error(TAG, "Server error: " + raw);
        }
    }

    // ===== Legacy restart/next (chỉ mode fallback) =====
    private void restartLevel() {
        legacyState = null;
        joinSent = false;
        pingTimer = 0f;
        input.setEnabled(true);
        Gdx.app.log(TAG, "Restart (legacy) level=" + levelId);
    }

    private void nextLevel() {
        String next = nextCampaignLevel(levelId);
        if (next == null) return;
        levelId = next;
        legacyState = null;
        joinSent = false;
        pingTimer = 0f;
        input.setEnabled(true);
        Gdx.app.log(TAG, "Next (legacy) level=" + levelId);
    }

    public static String nextCampaignLevel(String current) {
        if ("campaign_1".equals(current)) return "campaign_2";
        if ("campaign_2".equals(current)) return "campaign_3";
        return null;
    }

    @Override public void resize(int width, int height) {
        if (renderer != null) renderer.resize(width, height);
    }

    @Override
    public void pause() {
        if (input != null) input.releaseAll();
    }

    @Override public void resume() {}

    @Override
    public void dispose() {
        if (input != null) input.releaseAll();
        if (session != null) session.dispose();
        if (socket != null) socket.disconnect();
        if (renderer != null) renderer.dispose();
        if (assets != null) assets.dispose();
    }
}
