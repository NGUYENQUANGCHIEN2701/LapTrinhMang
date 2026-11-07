package com.tankzors.security;

import com.tankzors.application.LeaderboardService;
import com.tankzors.application.PlayerProgressService;
import com.tankzors.application.PlayerShopService;
import com.tankzors.leaderboard.LeaderboardRepository;
import com.tankzors.progress.PlayerProgressRepository;
import com.tankzors.progress.PlayerShopStateRepository;
import com.tankzors.progress.PlayerTotalsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDeleteUserTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PlayerProgressService progress;
    @Autowired private PlayerShopService shop;
    @Autowired private LeaderboardService leaderboard;
    @Autowired private PlayerProgressRepository progressRepo;
    @Autowired private PlayerTotalsRepository totalsRepo;
    @Autowired private PlayerShopStateRepository shopRepo;
    @Autowired private LeaderboardRepository leaderboardRepo;

    @Test
    void deletingUserAlsoDeletesTheirGameData() throws Exception {
        UserEntity u = new UserEntity();
        u.setUsername("doomed-user");
        u.setPassword("x");
        u.setDisplayName("doomed-user");
        u.setRole("ROLE_USER");
        u.setEnabled(true);
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
        u = users.save(u);
        String playerId = "user:doomed-user";
        progress.markVictory(playerId, "campaign_1");
        shop.syncShopData(playerId, new PlayerShopService.SyncRequest(500, 3, "{}", "{}"));
        leaderboard.submitScore("doomed-user", "campaign_1", 900, 30, 50);

        mvc.perform(post("/admin/users/" + u.getId() + "/delete")).andExpect(redirectedUrl("/admin"));

        assertFalse(users.existsByUsername("doomed-user"));
        assertTrue(progressRepo.findBySessionId(playerId).isEmpty());
        assertTrue(totalsRepo.findByPlayerId(playerId).isEmpty());
        assertTrue(shopRepo.findByPlayerId(playerId).isEmpty());
        assertTrue(leaderboardRepo.findByPlayerNameAndMapId("doomed-user", "campaign_1").isEmpty());
    }
}
