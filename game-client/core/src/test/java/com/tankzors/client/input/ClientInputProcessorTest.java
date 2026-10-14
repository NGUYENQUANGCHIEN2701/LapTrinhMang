package com.tankzors.client.input;

import com.badlogic.gdx.Input;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientInputProcessorTest {

    @Test
    void releasingLatestDirectionResumesPreviouslyHeldDirection() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ClientInputProcessor input = new ClientInputProcessor(callbacks);
        input.keyDown(Input.Keys.W);
        input.keyDown(Input.Keys.D);
        input.keyUp(Input.Keys.D);

        assertEquals(List.of("UP:true", "RIGHT:true", "RIGHT:false", "UP:true"), callbacks.moves);
        assertEquals(0, callbacks.stops);
        input.keyUp(Input.Keys.W);
        assertEquals(1, callbacks.stops);
    }

    @Test
    void releasingOneAliasDoesNotReleaseTheOtherHeldKey() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ClientInputProcessor input = new ClientInputProcessor(callbacks);
        input.keyDown(Input.Keys.W);
        input.keyDown(Input.Keys.UP);
        input.keyUp(Input.Keys.W);

        assertEquals(List.of("UP:true"), callbacks.moves);
        assertEquals(0, callbacks.stops);
        input.keyUp(Input.Keys.UP);
        assertEquals(List.of("UP:true", "UP:false"), callbacks.moves);
        assertEquals(1, callbacks.stops);
    }

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

    @Test
    void holdingSpaceRepeatsShotsUntilKeyIsReleased() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ClientInputProcessor input = new ClientInputProcessor(callbacks);

        input.keyDown(Input.Keys.SPACE);
        input.keyDown(Input.Keys.SPACE);
        assertEquals(1, callbacks.shots, "OS key-repeat must not duplicate the initial shot");

        input.update(0.10f);
        input.update(0.14f);
        assertEquals(3, callbacks.shots, "Holding Space must keep firing at the repeat cadence");

        input.keyUp(Input.Keys.SPACE);
        input.update(1f);
        assertEquals(3, callbacks.shots, "Releasing Space must stop automatic fire");
    }

    @Test
    void enterOnResultScreenContinuesWithoutTouchingGameplay() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ClientInputProcessor input = new ClientInputProcessor(callbacks);

        input.keyDown(Input.Keys.ENTER);
        assertEquals(0, callbacks.continues, "Enter khi đang chơi không được nhảy màn");

        input.setEnabled(false);
        input.keyDown(Input.Keys.ENTER);
        assertEquals(1, callbacks.continues);
    }

    private static final class RecordingCallbacks implements ClientInputProcessor.Callbacks {
        int reversePressed;
        int reverseReleased;
        int back;
        int openMenu;
        int menuMoves;
        int confirmMenu;
        int continues;
        int shots;
        int stops;
        final List<String> moves = new ArrayList<>();
        boolean menuOpen;

        @Override public void onMove(ClientInputProcessor.Direction direction, boolean moving) {
            moves.add(direction + ":" + moving);
        }
        @Override public void onShoot() { shots++; }
        @Override public void onAllReleased() { stops++; }
        @Override public void onRestart() {}
        @Override public void onNextLevel() {}
        @Override public void onContinue() { continues++; }
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
