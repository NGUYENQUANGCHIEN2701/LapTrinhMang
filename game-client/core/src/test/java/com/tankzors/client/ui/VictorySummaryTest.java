package com.tankzors.client.ui;

import com.tankzors.client.model.GameStateSnapshot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VictorySummaryTest {
    @Test void formatsMissionTimeWithSecondsAndUnboundedMinutes() {
        assertEquals("0:00", VictorySummary.time(-1));
        assertEquals("0:28", VictorySummary.time(28));
        assertEquals("1:05", VictorySummary.time(65));
        assertEquals("61:01", VictorySummary.time(3661));
    }

    @Test void distinguishesMissionRewardsFromCurrentWallet() {
        GameStateSnapshot state = new GameStateSnapshot();
        state.coinsEarned = 45;
        state.starsEarned = 4;
        state.playerCoins = 145;
        state.playerStars = 9;
        state.elapsedSeconds = 28;
        state.aiKills = 6;
        state.hpRemain = 4;
        state.score = 800;
        String[][] rows = VictorySummary.rows(state);
        assertEquals("+45", rows[0][1]);
        assertEquals("+4", rows[1][1]);
        assertEquals("145", rows[2][1]);
        assertEquals("9", rows[3][1]);
        assertEquals("0:28", rows[4][1]);
        assertEquals("6", rows[5][1]);
        assertEquals("4 HP", rows[6][1]);
        assertEquals("800", rows[7][1]);
    }
}
