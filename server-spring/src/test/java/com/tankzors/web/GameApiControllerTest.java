package com.tankzors.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.tankzors.application.PlayerProgressService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PlayerProgressService progress;

    @Test
    @WithMockUser(username = "bootstrap-visual")
    void bootstrapSendsBuildsVisualGrid() throws Exception {
        mvc.perform(get("/api/game/bootstrap").param("levelId", "campaign_1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.map.visualGrid[3][3]").value(115));
    }

    @Test
    @WithMockUser(username = "bootstrap-mission")
    void bootstrapPassesMissionScriptThrough() throws Exception {
        progress.markVictory("user:bootstrap-mission", "campaign_1");
        mvc.perform(get("/api/game/bootstrap").param("levelId", "campaign_2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.map.mission.objective").value("DESTROY_TANKS"))
            .andExpect(jsonPath("$.map.mission.enemyWaves.maxOnField").value(1))
            .andExpect(jsonPath("$.map.mission.allies.followPlayer").value(true));
    }
}
