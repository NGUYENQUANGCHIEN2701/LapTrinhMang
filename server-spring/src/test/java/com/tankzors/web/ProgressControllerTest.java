package com.tankzors.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
    void getProgressUsesAccountAndIgnoresSpoofedHeader() throws Exception {
        mvc.perform(get("/api/progress").with(user("progress-user"))
                .header("X-Player-Session", "someone-else"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("user:progress-user"))
            .andExpect(jsonPath("$.unlockedLevels[0]").value("campaign_1"));
    }

    @Test
    void resetProgressReturnsDefault() throws Exception {
        mvc.perform(post("/api/progress/reset").with(user("reset-user")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unlockedLevels[0]").value("campaign_1"));
    }
}
