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
 * Phase 5 REST endpoint test cho ProgressController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProgressControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void getProgressReturnsCampaign1ByDefault() throws Exception {
        mvc.perform(get("/api/progress"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unlockedLevels[0]").value("campaign_1"));
    }

    @Test
    void getProgressWithExplicitSessionHeader() throws Exception {
        mvc.perform(get("/api/progress").header("X-Player-Session", "explicit-session-xyz"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("explicit-session-xyz"))
            .andExpect(jsonPath("$.unlockedLevels[0]").value("campaign_1"));
    }

    @Test
    void resetProgressReturnsDefault() throws Exception {
        mvc.perform(post("/api/progress/reset").header("X-Player-Session", "session-to-reset"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unlockedLevels[0]").value("campaign_1"));
    }
}
