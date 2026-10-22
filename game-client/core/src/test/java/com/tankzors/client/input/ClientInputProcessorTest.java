package com.tankzors.client.input;

import com.badlogic.gdx.Input;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    void qMenuHandlesNavigationAndClosing() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        ClientInputProcessor input = new ClientInputProcessor(callbacks);

        input.keyDown(Input.Keys.Q);
        assertEquals(1, callbacks.openMenu);
        callbacks.menuOpen = true;
        input.keyDown(Input.Keys.DOWN);
        input.keyDown(Input.Keys.S);
        input.keyDown(Input.Keys.UP);
        input.keyDown(Input.Keys.W);
        assertEquals(0, callbacks.menuMoves); // DOWN(+1) + S(+1) + UP(-1) + W(-1) = 0
        input.keyDown(Input.Keys.Q);
        assertEquals(1, callbacks.closeMenu);
        input.keyDown(Input.Keys.ESCAPE);
        assertEquals(2, callbacks.closeMenu);
        input.keyDown(Input.Keys.ENTER);
        assertEquals(1, callbacks.confirmMenu);
        input.keyDown(Input.Keys.SPACE);
        assertEquals(2, callbacks.confirmMenu);

        // Click on inventory row 2 (y = 48 + 2 * 34 = 116)
        input.touchDown(30, 116, 0, 0);
        assertEquals(2, callbacks.selectedRow);

        // Click outside inventory closes menu
        input.touchDown(250, 116, 0, 0);
        assertEquals(3, callbacks.closeMenu);
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

    @Test
    void singleUseItemsRequireAnotherPressInsteadOfHeldAutofire() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        callbacks.repeat = false;
        ClientInputProcessor input = new ClientInputProcessor(callbacks);
        input.keyDown(Input.Keys.SPACE);
        input.update(1);
        assertEquals(1, callbacks.shots);
        input.keyUp(Input.Keys.SPACE);
        input.keyDown(Input.Keys.SPACE);
        assertEquals(2, callbacks.shots);
    }

    @Test
    void enterConfirmsAnActiveAirStrikeAim() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        callbacks.aiming = true;
        ClientInputProcessor input = new ClientInputProcessor(callbacks);
        input.keyDown(Input.Keys.ENTER);
        assertEquals(1, callbacks.shots);
        assertEquals(0, callbacks.continues);
    }

    @Test
    void radarClosingKeysAreConsumedUntilReleaseAndCannotShootOrOpenInventory() {
        for (int key : new int[]{Input.Keys.Q, Input.Keys.SPACE, Input.Keys.Z, Input.Keys.ENTER}) {
            RecordingCallbacks callbacks = new RecordingCallbacks();
            callbacks.radarOpen = true;
            ClientInputProcessor input = new ClientInputProcessor(callbacks);
            input.keyDown(key);
            input.keyDown(key); // Browser key repeat while the closing key remains held.
            input.keyUp(key);
            input.update(1);
            assertEquals(1, callbacks.radarClosed);
            assertEquals(0, callbacks.shots);
            assertEquals(0, callbacks.openMenu);
            assertFalse(input.isMoving());
        }
    }

    @Test
    void radarBlocksMovementAndEscapeOpensPauseWithoutClosingRadar() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        callbacks.radarOpen = true;
        ClientInputProcessor input = new ClientInputProcessor(callbacks);
        input.keyDown(Input.Keys.W);
        input.keyUp(Input.Keys.W);
        assertFalse(input.isMoving());
        input.keyDown(Input.Keys.ESCAPE);
        input.keyUp(Input.Keys.ESCAPE);
        assertTrue(callbacks.radarPaused);
        assertTrue(callbacks.radarOpen);
        input.keyDown(Input.Keys.ENTER);
        input.keyUp(Input.Keys.ENTER);
        assertFalse(callbacks.radarPaused);
        assertTrue(callbacks.radarOpen);
        assertEquals(0, callbacks.shots);
        input.touchDown(410, 20, 0, 0);
        assertFalse(callbacks.radarOpen);
        assertEquals(0, callbacks.cycles);
    }

    @Test
    void enteringRadarFromInventoryDoesNotCloseItOnEnterKeyUp() {
        RecordingCallbacks callbacks = new RecordingCallbacks();
        callbacks.menuOpen = true;
        callbacks.selectRadar = true;
        ClientInputProcessor input = new ClientInputProcessor(callbacks);
        input.keyDown(Input.Keys.ENTER);
        input.keyUp(Input.Keys.ENTER);
        assertTrue(callbacks.radarOpen);
        assertEquals(0, callbacks.radarClosed);
        assertEquals(0, callbacks.shots);
    }

    private static final class RecordingCallbacks implements ClientInputProcessor.Callbacks {
        int reversePressed;
        int reverseReleased;
        int back;
        int openMenu;
        int closeMenu;
        int selectedRow = -1;
        int menuMoves;
        int confirmMenu;
        int continues;
        int shots;
        int stops;
        final List<String> moves = new ArrayList<>();
        boolean menuOpen;
        boolean radarOpen, radarPaused, selectRadar;
        int radarClosed, cycles;
        @Override public boolean isRadarOpen() { return radarOpen; }
        @Override public boolean isRadarPaused() { return radarPaused; }
        @Override public void onCloseRadar() { radarOpen = false; radarClosed++; }
        @Override public void onRadarPause() { radarPaused = !radarPaused; }
        @Override public void onCycleWeapon() { cycles++; }
        boolean repeat = true;
        boolean aiming;
        @Override public boolean isAiming() { return aiming; }
        @Override public boolean repeatsWhileHeld() { return repeat; }

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
        @Override public void onCloseInventoryMenu() { closeMenu++; }
        @Override public void onSelectInventoryRow(int relativeRow) { selectedRow = relativeRow; }
        @Override public void onMoveInventorySelection(int delta) { menuMoves += delta; }
        @Override public void onConfirmInventorySelection() { confirmMenu++; if (selectRadar) { radarOpen = true; menuOpen = false; } }
    }
}
