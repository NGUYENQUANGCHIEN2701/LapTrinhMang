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

    public enum Direction { UP, DOWN, LEFT, RIGHT }

    public interface Callbacks {
        void onMove(Direction direction, boolean moving);
        void onShoot();
        /** Phase G: phát khi không còn phím di chuyển nào được giữ (gửi STOP). */
        void onAllReleased();
        /** Phase G: chơi lại màn hiện tại sau khi trận đấu kết thúc. */
        void onRestart();
        /** Phase G: chuyển sang màn kế tiếp sau khi chiến thắng. */
        void onNextLevel();
        /** Bật/tắt nhanh âm thanh (mute/unmute). */
        void onToggleMute();
    }

    private final Callbacks callbacks;
    private final EnumSet<Direction> activeDirections = EnumSet.noneOf(Direction.class);
    private boolean enabled = true;

    public ClientInputProcessor(Callbacks callbacks) {
        this.callbacks = callbacks;
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

    @Override
    public boolean keyDown(int keycode) {
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
        if (isShootKey(keycode)) {
            callbacks.onShoot();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (!enabled) return true;
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
    }

    // ========== Unused InputProcessor methods ==========

    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }
}
