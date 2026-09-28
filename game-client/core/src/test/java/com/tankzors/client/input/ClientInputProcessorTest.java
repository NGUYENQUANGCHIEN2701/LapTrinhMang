package com.tankzors.client.input;

import com.badlogic.gdx.Input;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientInputProcessorTest {

    @Test
    void xEmitsReversePressAndRelease() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ClientInputProcessor input = new ClientInputProcessor(callbacks);

        input.keyDown(Input.Keys.X);
        input.keyDown(Input.Keys.X);
        input.keyUp(Input.Keys.X);

        assertEquals(1, callbacks.reversePressed);
        assertEquals(1, callbacks.reverseReleased);
    }

    @Test
    void escapeReturnsToCampaignsOnlyWhenGameplayIsDisabled() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ClientInputProcessor input = new ClientInputProcessor(callbacks);

        input.keyDown(Input.Keys.ESCAPE);
        assertEquals(0, callbacks.back);
        input.setEnabled(false);
        input.keyDown(Input.Keys.ESCAPE);

        assertEquals(1, callbacks.back);
    }

    @Test
    void qMenuOnlyClosesOnEnterAndRoutesArrowNavigation() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ClientInputProcessor input = new ClientInputProcessor(callbacks);

        input.keyDown(Input.Keys.Q);
        assertEquals(1, callbacks.openMenu);
        callbacks.menuOpen = true;
        input.keyDown(Input.Keys.DOWN);
        input.keyDown(Input.Keys.Q);
        input.keyDown(Input.Keys.ESCAPE);
        assertEquals(1, callbacks.menuMoves);
        assertEquals(0, callbacks.confirmMenu);
        input.keyDown(Input.Keys.ENTER);
        assertEquals(1, callbacks.confirmMenu);
    }

    private static final class RecordingCallbacks implements ClientInputProcessor.Callbacks {
        int reversePressed;
        int reverseReleased;
        int back;
        int openMenu;
        int menuMoves;
        int confirmMenu;
        boolean menuOpen;

        @Override public void onMove(ClientInputProcessor.Direction direction, boolean moving) {}
        @Override public void onShoot() {}
        @Override public void onAllReleased() {}
        @Override public void onRestart() {}
        @Override public void onNextLevel() {}
        @Override public void onReverse(boolean reversing) {
            if (reversing) reversePressed++;
            else reverseReleased++;
        }
        @Override public void onBackToCampaigns() { back++; }
        @Override public boolean isInventoryMenuOpen() { return menuOpen; }
        @Override public void onOpenInventoryMenu() { openMenu++; }
        @Override public void onMoveInventorySelection(int delta) { menuMoves += delta; }
        @Override public void onConfirmInventorySelection() { confirmMenu++; }
    }
}
