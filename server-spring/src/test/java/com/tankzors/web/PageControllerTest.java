package com.tankzors.web;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import com.tankzors.application.PlayerProgressService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * MVC test cho Thymeleaf website shell (plan section 11).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PageControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PlayerProgressService progressService;

    @Autowired
    private com.tankzors.leaderboard.LeaderboardRepository leaderboardRepository;

    @Test
    void getRootReturnsMenuPage() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("menu"))
            .andExpect(content().string(containsString("TANKZORS")))
            .andExpect(content().string(containsString("Game")));
    }

    @Test
    void getMenuAliasReturnsMenuPage() throws Exception {
        mvc.perform(get("/menu"))
            .andExpect(status().isOk())
            .andExpect(view().name("menu"));
    }

    @Test
    void getGameMenuReturnsGameMenuPage() throws Exception {
        mvc.perform(get("/game-menu"))
            .andExpect(status().isOk())
            .andExpect(view().name("game-menu"))
            .andExpect(content().string(containsString("Campaigns")))
            .andExpect(model().attribute("continueAvailable", true))
            .andExpect(model().attribute("continueLevelId", "campaign_1"))
            .andExpect(content().string(not(containsString("Load Game"))))
            .andExpect(content().string(not(containsString("level mặc định"))))
            .andExpect(content().string(containsString("Coming soon")));
    }

    @Test
    void getCampaignsShowsThreeLevels() throws Exception {
        mvc.perform(get("/campaigns"))
            .andExpect(status().isOk())
            .andExpect(view().name("campaigns"))
            .andExpect(model().attribute("levels", hasSize(3)))
            .andExpect(model().attribute("unlockedLevels", hasItem("campaign_1")))
            .andExpect(content().string(containsString("/play?levelId=campaign_1")));
    }

    @Test
    @WithMockUser(username = "page-unlock")
    void campaignsPageReadsProgressOfLoggedInAccount() throws Exception {
        // Kết quả thắng được lưu theo user:<tên>; trang chọn màn phải đọc cùng khoá đó.
        progressService.markVictory("user:page-unlock", "campaign_1");
        mvc.perform(get("/campaigns"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("unlockedLevels", hasItem("campaign_2")))
            .andExpect(model().attribute("completedLevels", hasItem("campaign_1")))
            .andExpect(content().string(containsString("Đã hoàn thành")))
            .andExpect(content().string(not(containsString("/play?levelId=campaign_1"))));
        mvc.perform(get("/api/progress"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("user:page-unlock"))
            .andExpect(jsonPath("$.unlockedLevels", hasItem("campaign_2")));
    }

    @Test
    void getPlayDefaultsToCampaign1() throws Exception {
        mvc.perform(get("/play"))
            .andExpect(status().isOk())
            .andExpect(view().name("play"))
            .andExpect(content().string(containsString("data-level-id=\"campaign_1\"")))
            .andExpect(content().string(containsString("game-container")))
            .andExpect(content().string(containsString("/game/libgdx/index.html?levelId=campaign_1")));
    }

    @Test
    @WithMockUser(username = "page-select-level-2")
    void getPlayWithLevelIdSelectsThatLevel() throws Exception {
        progressService.reset("user:page-select-level-2");
        progressService.markVictory("user:page-select-level-2", "campaign_1");
        mvc.perform(get("/play").param("levelId", "campaign_2"))
            .andExpect(status().isOk())
            .andExpect(view().name("play"))
            .andExpect(content().string(containsString("data-level-id=\"campaign_2\"")))
            .andExpect(content().string(containsString("/game/libgdx/index.html?levelId=campaign_2")));
    }

    @Test
    @WithMockUser(username = "continue-level-2")
    void continueOpensCurrentAccountLevel2() throws Exception {
        progressService.reset("user:continue-level-2");
        progressService.markVictory("user:continue-level-2", "campaign_1");
        mvc.perform(get("/game-menu"))
            .andExpect(model().attribute("continueLevelId", "campaign_2"));
        mvc.perform(get("/play"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("selectedLevelId", "campaign_2"));
    }

    @Test
    @WithMockUser(username = "continue-level-3")
    void continueRechecksProgressEvenIfMenuAndAnonymousSessionAreStale() throws Exception {
        String playerId = "user:continue-level-3";
        progressService.reset(playerId);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("tankzors.playerId", "stale-anonymous-session");
        mvc.perform(get("/game-menu").session(session))
            .andExpect(model().attribute("continueLevelId", "campaign_1"));

        progressService.markVictory(playerId, "campaign_1");
        progressService.markVictory(playerId, "campaign_2");
        mvc.perform(get("/play").session(session))
            .andExpect(status().isOk())
            .andExpect(model().attribute("selectedLevelId", "campaign_3"))
            .andExpect(content().string(containsString("/game/libgdx/index.html?levelId=campaign_3")));
        mvc.perform(get("/game-menu").session(session))
            .andExpect(model().attribute("continueLevelId", "campaign_3"));
        mvc.perform(get("/api/game/bootstrap").session(session).param("levelId", "campaign_3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.levelId").value("campaign_3"));
    }

    @Test
    @WithMockUser(username = "continue-completed")
    void completedCampaignDisablesContinueInsteadOfReopeningLevel1() throws Exception {
        String playerId = "user:continue-completed";
        progressService.reset(playerId);
        for (String levelId : PlayerProgressService.CAMPAIGN_ORDER) {
            progressService.markVictory(playerId, levelId);
        }
        mvc.perform(get("/game-menu"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("continueAvailable", false))
            .andExpect(content().string(containsString("Bạn đã hoàn thành toàn bộ chiến dịch")))
            .andExpect(content().string(not(containsString("href=\"/play\""))));
        mvc.perform(get("/play"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns"));
    }

    @Test
    @WithMockUser(username = "continue-guard")
    void explicitCompletedAndLockedLevelsRedirectBeforeLoadingGame() throws Exception {
        String playerId = "user:continue-guard";
        progressService.reset(playerId);
        progressService.markVictory(playerId, "campaign_1");
        for (String levelId : new String[] {"campaign_1", "campaign_3"}) {
            mvc.perform(get("/play").param("levelId", levelId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/campaigns"))
                .andExpect(flash().attributeExists("flashError"));
        }
    }

    @Test
    void libgdxBootstrapIsServedAsStaticResource() throws Exception {
        Assumptions.assumeTrue(
            new ClassPathResource("static/game/libgdx/tankzorshtml/tankzorshtml.nocache.js").exists(),
            "LibGDX bundle chưa build; chạy html:copyToServer trước khi test");
        mvc.perform(get("/game/libgdx/tankzorshtml/tankzorshtml.nocache.js"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("tankzorshtml")));
    }

    @Test
    void getPlayWithUnknownLevelRedirectsToCampaigns() throws Exception {
        mvc.perform(get("/play").param("levelId", "campaign_999"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns"));
    }

    @Test
    void getSettingsReturnsForm() throws Exception {
        mvc.perform(get("/settings"))
            .andExpect(status().isOk())
            .andExpect(view().name("settings"))
            .andExpect(content().string(containsString("name=\"musicVolume\"")))
            .andExpect(content().string(containsString("name=\"soundVolume\"")))
            .andExpect(content().string(not(containsString("name=\"gameScale\""))))
            .andExpect(content().string(not(containsString("name=\"language\""))))
            .andExpect(content().string(not(containsString("name=\"showFps\""))));
    }

    @Test
    void postSettingsClampsAndRedirects() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/settings")
                .session(session)
                .param("musicVolume", "150")
                .param("soundVolume", "-5"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/settings"));

        MvcResult result = mvc.perform(get("/settings").session(session))
            .andExpect(status().isOk())
            .andReturn();
        String html = result.getResponse().getContentAsString();
        // volume clamped 150 -> 100, sound -5 -> 0
        assertTrue(html.contains("value=\"100\""), "musicVolume should be clamped to 100");
        assertTrue(html.contains("value=\"0\""), "soundVolume should be clamped to 0");
    }

    @Test
    void getLoadGameRendersPlaceholderSlots() throws Exception {
        mvc.perform(get("/load-game"))
            .andExpect(status().isOk())
            .andExpect(view().name("load-game"))
            .andExpect(content().string(not(containsString("No save system configured"))))
            .andExpect(content().string(containsString("Slot 1")))
            .andExpect(content().string(containsString("Slot 3")));
    }

    @Test
    void getLeaderboardRendersPageAndMapTabs() throws Exception {
        // H2 dùng chung giữa các test class: dọn để kiểm tra trạng thái bảng rỗng.
        leaderboardRepository.deleteAll();
        mvc.perform(get("/leaderboard"))
            .andExpect(status().isOk())
            .andExpect(view().name("leaderboard"))
            .andExpect(content().string(containsString("Campaign 1")))
            .andExpect(content().string(containsString("Campaign 2")))
            .andExpect(content().string(containsString("Campaign 3")))
            .andExpect(content().string(not(containsString("<th>Map</th>"))))
            .andExpect(content().string(containsString("colspan=\"6\"")));

        mvc.perform(get("/leaderboard").param("mapId", "campaign_2"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/leaderboard?mapId=campaign_2")));
    }

    @Test
    void unknownRouteReturns404() throws Exception {
        mvc.perform(get("/route-khong-ton-tai"))
            .andExpect(status().isNotFound());
    }

    @Test
    void error404RouteReturnsNotFoundPage() throws Exception {
        mvc.perform(get("/error/404"))
            .andExpect(status().isOk())
            .andExpect(view().name("error/404"));
    }

    @Test
    void oldCampaignRouteRedirectsToCampaigns() throws Exception {
        mvc.perform(get("/campaign"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns"));
    }
}
