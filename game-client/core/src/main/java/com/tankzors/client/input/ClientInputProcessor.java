package com.tankzors.client.input;

import java.util.EnumSet;

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
        default void onMoveInventorySelection(int delta) {}
        default void onConfirmInventorySelection() {}
        /** Bật/tắt nhanh âm thanh (mute/unmute). */
        default void onToggleMute() {}
        default void onBackToCampaigns() {}
    }

    private final Callbacks callbacks;
    private final EnumSet<Direction> activeDirections = EnumSet.noneOf(Direction.class);
    private boolean reverseActive;
    private boolean spaceActive;
    private float autoFireElapsed;
    private boolean enabled = true;
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
        if (!enabled || !spaceActive || callbacks.isInventoryMenuOpen()
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
        if (shopOverlay != null && shopOverlay.isVisible()) {
            return shopOverlay.handleKeyDown(keycode);
        }

        if (callbacks.isInventoryMenuOpen()) {
            if (keycode == Input.Keys.UP) callbacks.onMoveInventorySelection(-1);
            else if (keycode == Input.Keys.DOWN) callbacks.onMoveInventorySelection(1);
            else if (keycode == Input.Keys.ENTER) callbacks.onConfirmInventorySelection();
            // Theo hợp đồng UI: Q/Esc không đóng menu; mọi gameplay input đều bị chặn.
            return true;
        }

        if (keycode == Input.Keys.B) {
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
            if (keycode == Input.Keys.ESCAPE) {
                callbacks.onBackToCampaigns();
                return true;
            }
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
            if (!activeDirections.contains(direction)) {
                activeDirections.add(direction);
                callbacks.onMove(direction, true);
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
            if (activeDirections.contains(direction)) {
                activeDirections.remove(direction);
                callbacks.onMove(direction, false);
                if (activeDirections.isEmpty()) {
                    callbacks.onAllReleased();
                }
            }
            return true;
        }
        return false;
    }

    public void releaseAll() {
        boolean releasedAny = !activeDirections.isEmpty();
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

    // ========== InputProcessor touch/scroll methods ==========

    @Override public boolean keyTyped(char character) { return false; }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (shopOverlay != null && shopOverlay.isVisible()) {
            return shopOverlay.handleTouchDown(screenX, screenY);
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
        return false;
    }
}
