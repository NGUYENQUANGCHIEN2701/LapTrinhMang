package com.tankzors.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.tankzors.client.assets.AssetRepository;
import com.tankzors.client.audio.AudioManager;
import com.tankzors.client.input.ClientInputProcessor;
import com.tankzors.client.input.ClientInputProcessor.Direction;
import com.tankzors.client.network.GameSocket;
import com.tankzors.client.render.GameRenderer;
import com.tankzors.client.session.GameSession;
import com.tankzors.client.session.LocalCampaignSession;
import com.tankzors.client.ui.ShopOverlay;

/**
 * LibGDX entry point. Điều phối asset + render + input; delegate gameplay cho
 * {@link GameSession} (LocalCampaign / Host / Guest).
 */
public class GameClient implements ApplicationListener {

    private static final String TAG = "GameClient";

    public static final String DEFAULT_WS_URL = "ws://localhost:8081/game";
    public static final String DEFAULT_LEVEL = "campaign_1";

    private final String wsUrl;
    private String levelId;
    private final GameSocket socket;

    private GameSession session;

    private AssetRepository assets;
    private AudioManager audio;
    private GameRenderer renderer;
    private ShopOverlay shopOverlay;
    private ClientInputProcessor input;
    private Runnable backToCampaignsHandler;

    public GameClient(GameSession session) {
        this(null, DEFAULT_LEVEL, null);
        this.session = session;
    }

    public GameClient(String wsUrl, String levelId, GameSocket socket) {
        this.wsUrl = wsUrl;
        this.levelId = levelId;
        this.socket = socket;
    }

    public GameSocket getSocket() { return socket; }

    public void setSession(GameSession session) {
        this.session = session;
    }

    public AudioManager getAudio() { return audio; }
    public void setAudio(AudioManager audio) { this.audio = audio; }

    public String getLevelId() { return levelId; }
    public void setLevelId(String levelId) { this.levelId = levelId; }
    public void setBackToCampaignsHandler(Runnable handler) { this.backToCampaignsHandler = handler; }

    @Override
    public void create() {
        Gdx.app.log(TAG, "Boot - wsUrl=" + wsUrl + " level=" + levelId + " session=" + (session != null));

        assets = new AssetRepository();
        assets.loadAll();

        if (audio == null) {
            audio = new AudioManager();
        }
        audio.loadAll();

        if (session instanceof LocalCampaignSession) {
            ((LocalCampaignSession) session).setAudioManager(audio);
        }
        renderer = new GameRenderer(assets);
        renderer.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        if (session != null && session.getShopState() != null) {
            shopOverlay = new ShopOverlay(assets, audio, session.getShopState(), new ShopOverlay.ShopListener() {
                @Override
                public void onShopClosed() {
                    if (session instanceof LocalCampaignSession) {
                        ((LocalCampaignSession) session).syncShopState();
                    }
                }
                @Override
                public void onShopStateChanged() {
                    if (session instanceof LocalCampaignSession) {
                        ((LocalCampaignSession) session).syncShopState();
                    }
                }
                @Override public void onShopVisibilityChanged(boolean visible) {
                    if (session != null) session.setShopOpen(visible);
                }
                @Override public void onItemBought(com.tankzors.client.shop.ShopItem item) {
                    if (session != null) session.onWorkshopItemBought(item);
                }
                @Override public void onItemSold(com.tankzors.client.shop.ShopItem item) {
                    if (session != null) session.onWorkshopItemSold(item);
                }
                @Override public int missingTankHp() { return session != null ? session.workshopMissingHp() : 0; }
                @Override public boolean repairTank() { return session != null && session.repairTankAtWorkshop(); }
            });
        }

        if (session != null && session.requiresSocket() && socket != null) {
            socket.connect();
        }

        input = new ClientInputProcessor(new ClientInputProcessor.Callbacks() {
            @Override public void onMove(Direction direction, boolean moving) {
                if (session != null) session.onMove(direction, moving);
            }
            @Override public void onShoot() {
                if (session != null) session.onShoot();
            }
            @Override public void onAllReleased() {
                if (session != null) session.onAllReleased();
            }
            @Override public void onReverse(boolean reversing) {
                if (session != null) session.onReverse(reversing);
            }
            @Override public void onRestart() {
                if (audio != null) audio.playUiClick();
                if (session != null) session.restart();
            }
            @Override public void onNextLevel() {
                if (audio != null) audio.playUiClick();
                if (session != null) session.nextLevel();
            }
            @Override public void onContinue() {
                if (session == null || !"VICTORY".equalsIgnoreCase(session.status())) return;
                if (audio != null) audio.playUiClick();
                if (session.hasNextLevel()) {
                    session.nextLevel();
                } else if (session.canOpenShop() && backToCampaignsHandler != null) {
                    backToCampaignsHandler.run();
                }
            }
            @Override public void onToggleShop() {
                if (shopOverlay != null && session != null && session.canOpenShop()) {
                    shopOverlay.toggle();
                }
            }
            @Override public void onCycleWeapon() {
                if (session != null) {
                    session.cycleWeapon();
                    if (audio != null) audio.playUiWeapSwitch();
                }
            }
            @Override public void onSelectWeapon(int weaponId) {
                if (session != null) {
                    session.selectWeapon(weaponId);
                    if (audio != null) audio.playUiWeapSwitch();
                }
            }
            @Override public boolean isInventoryMenuOpen() {
                return session != null && session.isInventoryMenuOpen();
            }
            @Override public void onOpenInventoryMenu() {
                if (session != null) session.openInventoryMenu();
                if (audio != null) audio.playUiWindow();
            }
            @Override public void onMoveInventorySelection(int delta) {
                if (session != null) session.moveInventorySelection(delta);
                if (audio != null) audio.playUiWeapSwitch();
            }
            @Override public void onConfirmInventorySelection() {
                if (session != null) session.confirmInventorySelection();
                if (audio != null) audio.playUiClick();
            }
            @Override public void onToggleMute() {
                if (audio != null) audio.toggleMute();
            }
            @Override public void onBackToCampaigns() {
                if (session != null && session.canOpenShop() && backToCampaignsHandler != null) {
                    backToCampaignsHandler.run();
                }
            }
        });
        input.setShopOverlay(shopOverlay);
        Gdx.input.setInputProcessor(input);
        catchGameplayKeys();

        if (session != null) session.start();
    }

    private void catchGameplayKeys() {
        int[] keys = {
                Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT,
                Input.Keys.W, Input.Keys.A, Input.Keys.S, Input.Keys.D,
                Input.Keys.X,
                Input.Keys.SPACE, Input.Keys.J, Input.Keys.R, Input.Keys.N,
                Input.Keys.B, Input.Keys.Q, Input.Keys.E, Input.Keys.ESCAPE,
                Input.Keys.ENTER,
                Input.Keys.M,
                Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4, Input.Keys.NUM_5, Input.Keys.NUM_6
        };
        for (int key : keys) {
            Gdx.input.setCatchKey(key, true);
        }
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();

        if (input != null) input.update(dt);

        if (session != null) {
            session.update(dt);
            setEnabledByStatus(session.status());
            if (renderer != null) {
                renderer.render(session.snapshot());
                renderer.renderShop(shopOverlay);
            }
        } else if (renderer != null) {
            renderer.render(null);
            renderer.renderShop(shopOverlay);
        }
    }

    private void setEnabledByStatus(String status) {
        if (input == null) return;
        boolean playable = "PLAYING".equalsIgnoreCase(status);
        if (playable != input.isEnabled()) input.setEnabled(playable);
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
        if (audio != null) audio.dispose();
    }
}
