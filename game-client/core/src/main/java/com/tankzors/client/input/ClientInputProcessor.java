package com.tankzors.client.input;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

/**
 * Input handler dùng chung cho các LibGDX session.
 *
 * <p>Bàn phím (WASD hoặc mũi tên) cho di chuyển; Space/J cho shoot.</p>
 *
 * <p>Callback chỉ phát khi có transition (giữ phím không spam). Tương đương
 * Callback chỉ phát khi trạng thái phím thay đổi.</p>
 */
public final class ClientInputProcessor implements InputProcessor {

    private static final float AUTO_FIRE_REPEAT_SECONDS = 0.10f;

    public enum Direction { UP, DOWN, LEFT, RIGHT }

    public interface Callbacks {
        void onMove(Direction direction, boolean moving);
        void onShoot();
        default boolean isRadarOpen() { return false; }
        default boolean isRadarPaused() { return false; }
        default void onCloseRadar() {}
        default void onRadarPause() {}
        default boolean isAiming() { return false; }
        default boolean repeatsWhileHeld() { return true; }
        default void onReverse(boolean reversing) {}
        /** Phase G: phát khi không còn phím di chuyển nào được giữ (gửi STOP). */
        void onAllReleased();
        /** Phase G: chơi lại màn hiện tại sau khi trận đấu kết thúc. */
        void onRestart();
        /** Phase G: chuyển sang màn kế tiếp sau khi chiến thắng. */
        void onNextLevel();
        default void onToggleShop() {}
        default void onCycleWeapon() {}
        default void onSelectWeapon(int weaponId) {}
        default boolean isInventoryMenuOpen() { return false; }
        default void onOpenInventoryMenu() {}
        default void onCloseInventoryMenu() {}
        default void onMoveInventorySelection(int delta) {}
        default void onSelectInventoryRow(int relativeRow) {}
        default void onConfirmInventorySelection() {}
        /** Enter ở màn hình kết quả: sang màn kế (hoặc về chọn màn nếu hết). */
        default void onContinue() {}
        /** Bật/tắt nhanh âm thanh (mute/unmute). */
        default void onToggleMute() {}
        default void onBackToCampaigns() {}
        default boolean onResultKey(int key) { return false; }
        default boolean onResultTouch(float x, float y) { return false; }
        default boolean onResultScroll(float amount) { return false; }
    }

    private final Callbacks callbacks;
    private final EnumSet<Direction> activeDirections = EnumSet.noneOf(Direction.class);
    /** Physical keys in press order: aliases and fallback directions stay held. */
    private final List<Integer> movementKeys = new ArrayList<>();
    private boolean reverseActive;
    private boolean spaceActive;
    private float autoFireElapsed;
    private boolean enabled = true;
    private int radarMenuSelection;
    private boolean radarSettingsOpen;
    public int radarMenuSelection() { return radarMenuSelection; }
    public boolean radarSettingsOpen() { return radarSettingsOpen; }
    private void radarMenuAction() {
        switch (radarMenuSelection) {
            case 0: callbacks.onRadarPause(); break;
            case 1: callbacks.onToggleShop(); break;
            case 2: radarSettingsOpen = true; break;
            case 3: callbacks.onBackToCampaigns(); break;
        }
    }
    private final java.util.Set<Integer> consumedRadarKeys = new java.util.HashSet<>();
    private com.tankzors.client.ui.ShopOverlay shopOverlay;

    public ClientInputProcessor(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void setShopOverlay(com.tankzors.client.ui.ShopOverlay shopOverlay) {
        this.shopOverlay = shopOverlay;
    }

    public EnumSet<Direction> activeDirections() {
        return EnumSet.copyOf(activeDirections);
    }

    public boolean isMoving() {
        return !activeDirections.isEmpty();
    }

    public void setEnabled(boolean enabled) {
        if (!enabled) releaseAll();
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private Direction mapKeyToDirection(int keycode) {
        switch (keycode) {
            case Input.Keys.W:
            case Input.Keys.UP:
                return Direction.UP;
            case Input.Keys.S:
            case Input.Keys.DOWN:
                return Direction.DOWN;
            case Input.Keys.A:
            case Input.Keys.LEFT:
                return Direction.LEFT;
            case Input.Keys.D:
            case Input.Keys.RIGHT:
                return Direction.RIGHT;
            default:
                return null;
        }
    }

    private boolean isShootKey(int keycode) {
        return keycode == Input.Keys.SPACE || keycode == Input.Keys.J;
    }

    /** Keep requesting shots while Space is held; the tank cooldown controls actual fire rate. */
    public void update(float deltaSeconds) {
        if (!enabled || callbacks.isRadarOpen() || !spaceActive || !callbacks.repeatsWhileHeld() || callbacks.isInventoryMenuOpen()
                || (shopOverlay != null && shopOverlay.isVisible())) {
            autoFireElapsed = 0f;
            return;
        }
        autoFireElapsed += Math.max(0f, deltaSeconds);
        if (autoFireElapsed >= AUTO_FIRE_REPEAT_SECONDS) {
            autoFireElapsed %= AUTO_FIRE_REPEAT_SECONDS;
            callbacks.onShoot();
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        if (consumedRadarKeys.contains(keycode)) return true;
        if (shopOverlay != null && shopOverlay.isVisible()) {
            return shopOverlay.handleKeyDown(keycode);
        }
        if (callbacks.onResultKey(keycode)) return true;

        if (callbacks.isRadarOpen()) {
            releaseAll();
            if (callbacks.isRadarPaused()) {
                if (radarSettingsOpen) {
                    if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.ENTER) radarSettingsOpen = false;
                    else if (keycode == Input.Keys.M) callbacks.onToggleMute();
                } else if (keycode == Input.Keys.ESCAPE) callbacks.onRadarPause();
                else if (keycode == Input.Keys.UP || keycode == Input.Keys.W) radarMenuSelection = (radarMenuSelection + 3) % 4;
                else if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S) radarMenuSelection = (radarMenuSelection + 1) % 4;
                else if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) radarMenuAction();
                else if (keycode == Input.Keys.B) callbacks.onToggleShop();
                else if (keycode == Input.Keys.M) callbacks.onToggleMute();
            } else if (keycode == Input.Keys.ESCAPE) {
                radarMenuSelection = 0; radarSettingsOpen = false; callbacks.onRadarPause();
            } else if (keycode == Input.Keys.Q || keycode == Input.Keys.Z || keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER) callbacks.onCloseRadar();
            consumedRadarKeys.add(keycode);
            return true;
        }

        if (callbacks.isInventoryMenuOpen()) {
            if (keycode == Input.Keys.UP || keycode == Input.Keys.W || keycode == Input.Keys.LEFT || keycode == Input.Keys.A) {
                callbacks.onMoveInventorySelection(-1);
            } else if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S || keycode == Input.Keys.RIGHT || keycode == Input.Keys.D) {
                callbacks.onMoveInventorySelection(1);
            } else if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE || keycode == Input.Keys.J || keycode == Input.Keys.Z) {
                releaseAll();
                callbacks.onConfirmInventorySelection();
                consumedRadarKeys.add(keycode);
            } else if (keycode == Input.Keys.Q || keycode == Input.Keys.ESCAPE) {
                releaseAll();
                callbacks.onCloseInventoryMenu();
            }
            return true;
        }

        if (keycode == Input.Keys.B) {
            releaseAll();
            callbacks.onToggleShop();
            return true;
        }

        if (keycode == Input.Keys.M) {
            callbacks.onToggleMute();
            return true;
        }
        if (!enabled) {
            if (keycode == Input.Keys.R) {
                callbacks.onRestart();
                return true;
            }
            if (keycode == Input.Keys.N) {
                callbacks.onNextLevel();
                return true;
            }
            if (keycode == Input.Keys.ENTER) {
                callbacks.onContinue();
                return true;
            }
            if (keycode == Input.Keys.ESCAPE) {
                callbacks.onBackToCampaigns();
                return true;
            }
            return true;
        }

        if (keycode == Input.Keys.ENTER && callbacks.isAiming()) {
            callbacks.onShoot();
            return true;
        }
        if (keycode == Input.Keys.X) {
            if (!reverseActive) {
                reverseActive = true;
                callbacks.onReverse(true);
            }
            return true;
        }

        if (keycode == Input.Keys.Q) {
            releaseAll();
            callbacks.onOpenInventoryMenu();
            return true;
        }
        if (keycode == Input.Keys.NUM_1) {
            callbacks.onSelectWeapon(0);
            return true;
        }
        if (keycode == Input.Keys.NUM_2) {
            callbacks.onSelectWeapon(1);
            return true;
        }
        if (keycode == Input.Keys.NUM_3) {
            callbacks.onSelectWeapon(2);
            return true;
        }
        if (keycode == Input.Keys.NUM_4) {
            callbacks.onSelectWeapon(3);
            return true;
        }
        if (keycode == Input.Keys.NUM_5) {
            callbacks.onSelectWeapon(4);
            return true;
        }
        if (keycode == Input.Keys.NUM_6) {
            callbacks.onSelectWeapon(5);
            return true;
        }

        Direction direction = mapKeyToDirection(keycode);
        if (direction != null) {
            if (!movementKeys.contains(keycode)) {
                Direction previous = latestDirection();
                movementKeys.add(keycode);
                activeDirections.add(direction);
                if (previous != direction) callbacks.onMove(direction, true);
            }
            return true;
        }
        if (keycode == Input.Keys.SPACE) {
            if (!spaceActive) {
                spaceActive = true;
                autoFireElapsed = 0f;
                callbacks.onShoot();
            }
            return true;
        }
        if (isShootKey(keycode)) {
            callbacks.onShoot();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (consumedRadarKeys.remove(keycode)) return true;
        if (callbacks.isRadarOpen()) return true;
        if (keycode == Input.Keys.SPACE) {
            spaceActive = false;
            autoFireElapsed = 0f;
            return true;
        }
        if (shopOverlay != null && shopOverlay.isVisible()) {
            return true;
        }
        if (callbacks.isInventoryMenuOpen()) return true;
        if (!enabled) return true;
        if (keycode == Input.Keys.X) {
            if (reverseActive) {
                reverseActive = false;
                callbacks.onReverse(false);
            }
            return true;
        }
        Direction direction = mapKeyToDirection(keycode);
        if (direction != null) {
            Direction previous = latestDirection();
            if (movementKeys.remove(Integer.valueOf(keycode))) {
                boolean directionStillHeld = false;
                for (int key : movementKeys) {
                    if (mapKeyToDirection(key) == direction) directionStillHeld = true;
                }
                if (!directionStillHeld) {
                    activeDirections.remove(direction);
                    callbacks.onMove(direction, false);
                }
                Direction next = latestDirection();
                if (next == null) {
                    callbacks.onAllReleased();
                } else if (next != previous) {
                    callbacks.onMove(next, true);
                }
            }
            return true;
        }
        return false;
    }

    public void releaseAll() {
        boolean releasedAny = !activeDirections.isEmpty();
        movementKeys.clear();
        for (Direction d : Direction.values()) {
            if (activeDirections.contains(d)) {
                activeDirections.remove(d);
                callbacks.onMove(d, false);
            }
        }
        if (releasedAny) {
            callbacks.onAllReleased();
        }
        if (reverseActive) {
            reverseActive = false;
            callbacks.onReverse(false);
        }
        spaceActive = false;
        autoFireElapsed = 0f;
    }

    private Direction latestDirection() {
        return movementKeys.isEmpty() ? null : mapKeyToDirection(movementKeys.get(movementKeys.size() - 1));
    }

    // ========== InputProcessor touch/scroll methods ==========

    @Override public boolean keyTyped(char character) { return false; }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (shopOverlay != null && shopOverlay.isVisible()) {
            return shopOverlay.handleTouchDown(screenX, screenY);
        }
        if (callbacks.onResultTouch(screenX, screenY)) return true;
        if (callbacks.isInventoryMenuOpen()) {
            releaseAll();
            // HUD Panel nằm ở x: 8..166, y: 48..
            if (screenX >= 8 && screenX <= 166 && screenY >= 48) {
                int clickedRow = (screenY - 48) / 34;
                callbacks.onSelectInventoryRow(clickedRow);
            } else {
                callbacks.onCloseInventoryMenu();
            }
            return true;
        }
        if (callbacks.isRadarOpen()) {
            releaseAll();
            if (callbacks.isRadarPaused()) {
                if (radarSettingsOpen) radarSettingsOpen = false;
                else if (Gdx.graphics != null) {
                    int index = com.tankzors.client.render.RadarOverlay.menuHit(screenX, screenY, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    if (index >= 0) { radarMenuSelection = index; radarMenuAction(); }
                }
            } else callbacks.onCloseRadar();
            return true;
        }
        // Weapon Slot at (400, 10, 80, 45)
        if (screenX >= 400 && screenX <= 480 && screenY >= 10 && screenY <= 55) {
            callbacks.onCycleWeapon();
            return true;
        }
        return false;
    }

    @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (shopOverlay != null && shopOverlay.isVisible()) {
            return shopOverlay.handleScrolled(amountY);
        }
        if (callbacks.onResultScroll(amountY)) return true;
        return false;
    }
}
