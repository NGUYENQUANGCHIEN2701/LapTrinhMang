package com.tankzors.client.input;

import java.util.EnumSet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

/**
 * Input handler LibGDX parity với {@code InputHandler.ts} React.
 *
 * <p>Bàn phím (WASD hoặc mũi tên) cho di chuyển; Space/J cho shoot.</p>
 *
 * <p>Callback chỉ phát khi có transition (giữ phím không spam). Tương đương
 * React: chỉ gửi onMove/onShoot khi trạng thái pressed đổi.</p>
 */
public final class ClientInputProcessor implements InputProcessor {

    public enum Direction { UP, DOWN, LEFT, RIGHT }

    public interface Callbacks {
        void onMove(Direction direction, boolean moving);
        void onShoot();
    }

    private final Callbacks callbacks;
    private final EnumSet<Direction> activeDirections = EnumSet.noneOf(Direction.class);

    public ClientInputProcessor(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    public EnumSet<Direction> activeDirections() {
        return EnumSet.copyOf(activeDirections);
    }

    public boolean isMoving() {
        return !activeDirections.isEmpty();
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
        Direction direction = mapKeyToDirection(keycode);
        if (direction != null) {
            if (activeDirections.contains(direction)) {
                activeDirections.remove(direction);
                callbacks.onMove(direction, false);
            }
            return true;
        }
        return false;
    }

    public void releaseAll() {
        for (Direction d : Direction.values()) {
            if (activeDirections.contains(d)) {
                activeDirections.remove(d);
                callbacks.onMove(d, false);
            }
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
