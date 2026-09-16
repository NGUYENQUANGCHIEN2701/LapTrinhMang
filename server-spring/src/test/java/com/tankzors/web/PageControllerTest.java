package com.tankzors.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
            .andExpect(content().string(containsString("Load Game")))
            .andExpect(content().string(containsString("Coming soon")));
    }

    @Test
    void getCampaignsShowsThreeLevels() throws Exception {
        mvc.perform(get("/campaigns"))
            .andExpect(status().isOk())
            .andExpect(view().name("campaigns"))
            .andExpect(content().string(containsString("campaign_1")))
            .andExpect(content().string(containsString("campaign_2")))
            .andExpect(content().string(containsString("campaign_3")))
            .andExpect(content().string(containsString("/play?levelId=campaign_1")));
    }

    @Test
    void getPlayDefaultsToCampaign1() throws Exception {
        mvc.perform(get("/play"))
            .andExpect(status().isOk())
            .andExpect(view().name("play"))
            .andExpect(content().string(containsString("data-level-id=\"campaign_1\"")))
            .andExpect(content().string(containsString("game-container")));
    }

    @Test
    void getPlayWithLevelIdSelectsThatLevel() throws Exception {
        mvc.perform(get("/play").param("levelId", "campaign_2"))
            .andExpect(status().isOk())
            .andExpect(view().name("play"))
            .andExpect(content().string(containsString("data-level-id=\"campaign_2\"")));
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
            .andExpect(content().string(containsString("name=\"gameScale\"")))
            .andExpect(content().string(containsString("name=\"language\"")));
    }

    @Test
    void postSettingsClampsAndRedirects() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/settings")
                .session(session)
                .param("musicVolume", "150")
                .param("soundVolume", "-5")
                .param("language", "fr")
                .param("showFps", "on")
                .param("gameScale", "999"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/settings"));

        MvcResult result = mvc.perform(get("/settings").session(session))
            .andExpect(status().isOk())
            .andReturn();
        String html = result.getResponse().getContentAsString();
        // volume clamped 150 -> 100, sound -5 -> 0, scale 999 -> 200
        assertTrue(html.contains("value=\"100\""), "musicVolume should be clamped to 100");
        assertTrue(html.contains("value=\"0\""), "soundVolume should be clamped to 0");
        assertTrue(html.contains("value=\"200\""), "gameScale should be clamped to 200");
    }

    @Test
    void getLoadGameRendersPlaceholderSlots() throws Exception {
        mvc.perform(get("/load-game"))
            .andExpect(status().isOk())
            .andExpect(view().name("load-game"))
            .andExpect(content().string(containsString("No save system configured")))
            .andExpect(content().string(containsString("Slot 1")))
            .andExpect(content().string(containsString("Slot 3")));
    }

    @Test
    void getLeaderboardRendersPageAndMapTabs() throws Exception {
        mvc.perform(get("/leaderboard"))
            .andExpect(status().isOk())
            .andExpect(view().name("leaderboard"))
            .andExpect(content().string(containsString("campaign_1")))
            .andExpect(content().string(containsString("campaign_2")))
            .andExpect(content().string(containsString("campaign_3")));

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
