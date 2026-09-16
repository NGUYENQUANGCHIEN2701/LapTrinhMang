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

/**
 * LibGDX entry point: ApplicationListener điều phối asset + ws + render + input.
 *
 * <p>Tương đương {@code GameCanvas.tsx} + {@code GameLoop.ts} của React client.
 * Lifecycle:</p>
 * <ol>
 *   <li>{@code create()}: load asset, kết nối WS, đăng ký input, gửi JOIN_GAME.</li>
 *   <li>{@code render()}: poll WS message -> parse -> update snapshot -> render.</li>
 *   <li>{@code dispose()}: giải phóng asset, đóng WS.</li>
 * </ol>
 *
 * <p>Không phụ thuộc transport cụ thể: WebSocket được inject qua
 * {@link GameSocket} (desktop = Java-WebSocket, html = browser native).</p>
 *
 * <p>GWT-compatible: tránh SLF4J/Reflection; log qua {@code Gdx.app.log}.</p>
 */
public class GameClient implements ApplicationListener {

    private static final String TAG = "GameClient";

    public static final String DEFAULT_WS_URL = "ws://localhost:8081/game";
    public static final String DEFAULT_LEVEL = "campaign_1";

    /** Phase G: heartbeat PING mỗi 10s để giữ kết nối + đo RTT qua PONG. */
    private static final float PING_INTERVAL_SEC = 10f;

    private final String wsUrl;
    private String levelId;
    private final GameSocket socket;

    private AssetRepository assets;
    private GameRenderer renderer;
    private StatePacketParser parser;
    private ClientInputProcessor input;

    private GameStateSnapshot state;
    private boolean joinSent;

    private float pingTimer;
    private long lastPingSentMs;

    /**
     * @param wsUrl   URL endpoint {@code ws://host:port/game}.
     * @param levelId mã level campaign (vd {@code campaign_1}).
     * @param socket  transport (desktop hoặc html).
     */
    public GameClient(String wsUrl, String levelId, GameSocket socket) {
        this.wsUrl = wsUrl;
        this.levelId = levelId;
        this.socket = socket;
    }

    @Override
    public void create() {
        Gdx.app.log(TAG, "Boot - wsUrl=" + wsUrl + " level=" + levelId);

        assets = new AssetRepository();
        assets.loadAll();

        renderer = new GameRenderer(assets);
        renderer.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        parser = new StatePacketParser();
        socket.connect();

        input = new ClientInputProcessor(new ClientInputProcessor.Callbacks() {
            @Override
            public void onMove(Direction direction, boolean moving) {
                socket.send(PacketBuilder.moveInput(direction.name(), moving));
            }

            @Override
            public void onShoot() {
                socket.send(PacketBuilder.shootInput());
            }

            @Override
            public void onAllReleased() {
                socket.send(PacketBuilder.stop());
            }

            @Override
            public void onRestart() {
                restartLevel();
            }

            @Override
            public void onNextLevel() {
                nextLevel();
            }
        });
        Gdx.input.setInputProcessor(input);
    }

    @Override
    public void render() {
        // 1. Poll tất cả WS message đến
        String raw;
        while ((raw = socket.poll()) != null) {
            handleIncoming(raw);
        }

        // 2. Khi vừa connect, gửi JOIN_GAME 1 lần.
        if (socket.isConnected() && !joinSent) {
            socket.send(PacketBuilder.joinGameCampaign(levelId));
            joinSent = true;
        }

        // 3. Phase G: heartbeat PING định kỳ khi đang kết nối.
        if (socket.isConnected()) {
            pingTimer += Gdx.graphics.getDeltaTime();
            if (pingTimer >= PING_INTERVAL_SEC) {
                pingTimer = 0f;
                lastPingSentMs = System.currentTimeMillis();
                socket.send(PacketBuilder.ping());
            }
        }

        // 4. Render state mới nhất
        if (renderer != null) {
            renderer.render(state);
        }
    }

    private void handleIncoming(String raw) {
        String type = parser.parseType(raw);
        if (type == null) return;
        if ("STATE".equals(type)) {
            try {
                GameStateSnapshot parsed = parser.parseState(raw);
                if (this.state == null) {
                    Gdx.app.log(TAG, "first STATE received: status=" + parsed.gameStatus
                        + " tanks=" + parsed.tanks.size() + " bullets=" + parsed.bullets.size());
                }
                this.state = parsed;
                if (input != null) {
                    boolean playable = "PLAYING".equalsIgnoreCase(parsed.gameStatus);
                    input.setEnabled(playable);
                }
            } catch (Exception e) {
                Gdx.app.error(TAG, "Parse STATE loi: " + e.getMessage());
            }
        } else if ("CONNECT".equals(type)) {
            // Phase G: server gán sessionId, dùng để khớp player tank trong STATE.
            String sessionId = parser.parseSessionId(raw);
            Gdx.app.log(TAG, "CONNECT received sessionId=" + sessionId);
        } else if ("PONG".equals(type)) {
            long now = System.currentTimeMillis();
            long rtt = lastPingSentMs > 0 ? (now - lastPingSentMs) : -1;
            Gdx.app.debug(TAG, "PONG rtt=" + rtt + "ms");
        } else if ("ACK".equals(type)) {
            Gdx.app.debug(TAG, "ACK " + raw);
        } else if ("ERROR".equals(type)) {
            Gdx.app.error(TAG, "Server error: " + raw);
        }
    }

    private void restartLevel() {
        state = null;
        joinSent = false;
        pingTimer = 0f;
        input.setEnabled(true);
        Gdx.app.log(TAG, "Restart level=" + levelId);
    }

    private void nextLevel() {
        String next = nextCampaignLevel(levelId);
        if (next == null) {
            Gdx.app.log(TAG, "No next level after " + levelId);
            return;
        }
        levelId = next;
        state = null;
        joinSent = false;
        pingTimer = 0f;
        input.setEnabled(true);
        Gdx.app.log(TAG, "Next level=" + levelId);
    }

    private static String nextCampaignLevel(String current) {
        if ("campaign_1".equals(current)) return "campaign_2";
        if ("campaign_2".equals(current)) return "campaign_3";
        return null;
    }

    @Override
    public void resize(int width, int height) {
        if (renderer != null) renderer.resize(width, height);
    }

    @Override
    public void pause() {
        if (input != null) input.releaseAll();
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        if (input != null) input.releaseAll();
        if (socket != null) socket.disconnect();
        if (renderer != null) renderer.dispose();
        if (assets != null) assets.dispose();
    }
}
