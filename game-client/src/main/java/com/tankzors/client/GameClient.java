package com.tankzors.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.TimeUtils;
import com.tankzors.client.assets.AssetRepository;
import com.tankzors.client.input.ClientInputProcessor;
import com.tankzors.client.input.ClientInputProcessor.Direction;
import com.tankzors.client.model.GameStateSnapshot;
import com.tankzors.client.network.PacketBuilder;
import com.tankzors.client.network.StatePacketParser;
import com.tankzors.client.network.WebSocketBridge;
import com.tankzors.client.render.GameRenderer;

/**
 * LibGDX entry point: ApplicationListener điều phối asset + ws + render + input.
 *
 * <p>Tương đương {@code GameCanvas.tsx} + {@code GameLoop.ts} của React client.
 * Lifecycle:</p>
 * <ol>
 *   <li>{@code create()}: load asset, kết nối WS, đăng ký input, gửi JOIN_GAME.</li>
 *   <li>{@code render()}: poll WS message -> parse -> update snapshot -> </li>
 *   <li>{@code dispose()}: giải phóng asset, đóng WS.</li>
 * </ol>
 *
 * <p>Phase 3 hiện cố định LEVEL = campaign_1. Phase sau sẽ thêm menu chọn
 * màn (chuyển sang Thymeleaf web).</p>
 */
public class GameClient implements ApplicationListener {

    private static final Logger logger = LoggerFactory.getLogger(GameClient.class);

    public static final String DEFAULT_WS_URL = "ws://localhost:8081/game";
    public static final String DEFAULT_LEVEL = "campaign_1";

    private final String wsUrl;
    private final String levelId;

    private AssetRepository assets;
    private GameRenderer renderer;
    private WebSocketBridge ws;
    private StatePacketParser parser;
    private ClientInputProcessor input;

    private GameStateSnapshot state;

    public GameClient() {
        this(DEFAULT_WS_URL, DEFAULT_LEVEL);
    }

    public GameClient(String wsUrl, String levelId) {
        this.wsUrl = wsUrl;
        this.levelId = levelId;
    }

    @Override
    public void create() {
        logger.info("[GameClient] Boot - wsUrl={} level={}", wsUrl, levelId);

        assets = new AssetRepository();
        assets.loadAll();

        renderer = new GameRenderer(assets);
        renderer.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        parser = new StatePacketParser();
        ws = new WebSocketBridge(wsUrl);
        ws.connect();

        input = new ClientInputProcessor(new ClientInputProcessor.Callbacks() {
            @Override
            public void onMove(Direction direction, boolean moving) {
                ws.sendRaw(PacketBuilder.moveInput(direction.name(), moving));
            }

            @Override
            public void onShoot() {
                ws.sendRaw(PacketBuilder.shootInput());
            }
        });
        Gdx.input.setInputProcessor(input);
    }

    @Override
    public void render() {
        // 1. Poll tất cả WS message đến
        String raw;
        while ((raw = ws.poll()) != null) {
            handleIncoming(raw);
        }

        // 2. Khi vừa connect, gửi JOIN_GAME 1 lần
        if (ws.isConnected() && ws.getSessionId() != null && state == null) {
            ws.sendRaw(PacketBuilder.joinGameCampaign(levelId));
        }

        // 3. Render state mới nhất
        if (renderer != null) {
            renderer.render(state);
        }
    }

    private void handleIncoming(String raw) {
        String type = parser.parseType(raw);
        if (type == null) return;
        switch (type) {
            case "CONNECT":
                String sid = parser.parseSessionId(raw);
                ws.setSessionId(sid);
                logger.info("[GameClient] sessionId={}", sid);
                break;
            case "STATE":
                try {
                    GameStateSnapshot parsed = parser.parseState(raw);
                    if (this.state == null) {
                        logger.info("[GameClient] first STATE received: status={} tanks={} bullets={}",
                            parsed.gameStatus, parsed.tanks.size(), parsed.bullets.size());
                    }
                    this.state = parsed;
                } catch (Exception e) {
                    logger.warn("[GameClient] Parse STATE lỗi: {}", e.getMessage());
                }
                break;
            case "ACK":
            case "PONG":
                // không cần xử lý giai đoạn này
                break;
            case "ERROR":
                logger.warn("[GameClient] Server error: {}", raw);
                break;
            default:
                logger.debug("[GameClient] Unhandled: {}", type);
        }
    }

    @Override
    public void resize(int width, int height) {
        if (renderer != null) renderer.resize(width, height);
    }

    @Override
    public void pause() {
        input.releaseAll();
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        if (input != null) input.releaseAll();
        if (ws != null) ws.disconnect();
        if (renderer != null) renderer.dispose();
        if (assets != null) assets.dispose();
    }
}
