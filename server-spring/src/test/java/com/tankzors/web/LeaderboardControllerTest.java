package com.tankzors.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.tankzors.application.LeaderboardService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST leaderboard: chỉ đọc. Điểm được ghi qua ResultService, không có API ghi trực tiếp.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private LeaderboardService leaderboardService;

    @Test
    void topScoresCanQuery() throws Exception {
        leaderboardService.submitScore("RestHeroUnique", "campaign_unique_99", 900, 40, 75);

        mvc.perform(get("/api/leaderboard/campaign_unique_99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.playerName == 'RestHeroUnique')].score").value(900));
    }

    @Test
    void directScoreWriteEndpointsAreGone() throws Exception {
        mvc.perform(post("/api/leaderboard")
                .contentType("application/json")
                .content("{\"playerName\":\"admin\",\"mapId\":\"campaign_1\",\"score\":999999}"))
            .andExpect(status().is4xxClientError());
        mvc.perform(post("/api/leaderboard/submit-from-client")
                .param("playerName", "admin").param("score", "999999"))
            .andExpect(status().is4xxClientError());
    }
}
