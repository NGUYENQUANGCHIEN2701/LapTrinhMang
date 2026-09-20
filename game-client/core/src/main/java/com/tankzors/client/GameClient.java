package com.tankzors.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.tankzors.client.assets.AssetRepository;
import com.tankzors.client.input.ClientInputProcessor;
import com.tankzors.client.input.ClientInputProcessor.Direction;
import com.tankzors.client.render.GameRenderer;
import com.tankzors.client.session.GameSession;

/**
 * LibGDX entry point for every client-authoritative game mode.
 * Rendering and keyboard input live here; simulation/network behavior belongs
 * to the selected {@link GameSession}.
 */
public class GameClient implements ApplicationListener {

    public static final String DEFAULT_LEVEL = "campaign_1";

    private final GameSession session;
    private AssetRepository assets;
    private GameRenderer renderer;
    private ClientInputProcessor input;

    public GameClient(GameSession session) {
        if (session == null) throw new IllegalArgumentException("session is required");
        this.session = session;
    }

    @Override
    public void create() {
        assets = new AssetRepository();
        assets.loadAll();
        renderer = new GameRenderer(assets);
        renderer.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        input = new ClientInputProcessor(new ClientInputProcessor.Callbacks() {
            @Override public void onMove(Direction direction, boolean moving) {
                session.onMove(direction, moving);
            }
            @Override public void onShoot() { session.onShoot(); }
            @Override public void onAllReleased() { session.onAllReleased(); }
            @Override public void onRestart() { session.restart(); }
            @Override public void onNextLevel() { session.nextLevel(); }
        });
        Gdx.input.setInputProcessor(input);
        catchGameplayKeys();
        session.start();
    }

    private void catchGameplayKeys() {
        int[] keys = {
                Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT,
                Input.Keys.W, Input.Keys.A, Input.Keys.S, Input.Keys.D,
                Input.Keys.SPACE, Input.Keys.J, Input.Keys.R, Input.Keys.N
        };
        for (int key : keys) Gdx.input.setCatchKey(key, true);
    }

    @Override
    public void render() {
        session.update(Gdx.graphics.getDeltaTime());
        if (input != null) {
            boolean playable = "PLAYING".equalsIgnoreCase(session.status());
            if (playable != input.isEnabled()) input.setEnabled(playable);
        }
        if (renderer != null) renderer.render(session.snapshot());
    }

    @Override public void resize(int width, int height) {
        if (renderer != null) renderer.resize(width, height);
    }

    @Override public void pause() {
        if (input != null) input.releaseAll();
    }

    @Override public void resume() {}

    @Override
    public void dispose() {
        if (input != null) input.releaseAll();
        session.dispose();
        if (renderer != null) renderer.dispose();
        if (assets != null) assets.dispose();
    }
}
