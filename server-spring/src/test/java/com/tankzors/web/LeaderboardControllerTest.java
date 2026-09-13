package com.tankzors.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 4 REST endpoint integration test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void submitScoreReturnsOkAndTopScoresCanQuery() throws Exception {
        mvc.perform(post("/api/leaderboard")
                .contentType("application/json")
                .content("{\"playerName\":\"RestHeroUnique\",\"mapId\":\"campaign_unique_99\",\"score\":900,\"timeSeconds\":40,\"hpRemain\":75}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OK"))
            .andExpect(jsonPath("$.score").value(900));

        mvc.perform(get("/api/leaderboard/campaign_unique_99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.playerName == 'RestHeroUnique')].score").value(900));
    }

    @Test
    void submitInvalidReturns400() throws Exception {
        mvc.perform(post("/api/leaderboard")
                .contentType("application/json")
                .content("{\"mapId\":\"campaign_2\",\"score\":900}"))
            .andExpect(status().isBadRequest());
    }
}
