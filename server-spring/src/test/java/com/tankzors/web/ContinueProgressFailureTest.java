package com.tankzors.web;

import com.tankzors.application.CampaignLevelView;
import com.tankzors.application.CampaignQueryService;
import com.tankzors.application.LeaderboardService;
import com.tankzors.application.PlayerProgressService;
import com.tankzors.identity.PlayerIdentityResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContinueProgressFailureTest {
    private final PlayerProgressService progress = mock(PlayerProgressService.class);
    private final CampaignQueryService campaigns = mock(CampaignQueryService.class);
    private final PlayerIdentityResolver identity = mock(PlayerIdentityResolver.class);
    private final PageController controller = new PageController(campaigns,
        mock(LeaderboardService.class), progress, mock(SettingsService.class),
        new PageModelFactory(), identity);
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    void unavailableProgressDisablesContinueWithoutGuessingLevel1() {
        when(identity.resolve(request)).thenReturn("user:continue-unavailable");
        when(progress.getContinueLevel("user:continue-unavailable"))
            .thenThrow(new IllegalStateException("Database unavailable"));
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("game-menu", controller.gameMenu(model, request));
        assertEquals(false, model.get("continueAvailable"));
        assertFalse(model.containsAttribute("continueLevelId"));
        assertEquals("Chưa tải được tiến độ. Vui lòng thử lại sau.", model.get("continueHint"));
        verifyNoInteractions(campaigns);
    }

    @Test
    void unavailableProgressRedirectsInsteadOfLoadingDefaultGame() {
        when(identity.resolve(request)).thenReturn("user:continue-unavailable");
        when(progress.getContinueLevel("user:continue-unavailable"))
            .thenThrow(new IllegalStateException("Database unavailable"));
        ExtendedModelMap model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/game-menu", controller.play(null, model, request, redirect));
        assertTrue(redirect.getFlashAttributes().containsKey("flashError"));
        assertFalse(model.containsAttribute("selectedLevelId"));
        verifyNoInteractions(campaigns);
    }

    @Test
    void explicitLevelCannotBypassUnavailableProgress() {
        when(identity.resolve(request)).thenReturn("user:continue-unavailable");
        when(campaigns.getLevel("campaign_3")).thenReturn(Optional.of(
            new CampaignLevelView("campaign_3", "Màn 3", "HARD", "Khó", 3, "")));
        when(progress.isUnlocked("user:continue-unavailable", "campaign_3"))
            .thenThrow(new IllegalStateException("Database unavailable"));
        ExtendedModelMap model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/game-menu", controller.play("campaign_3", model, request, redirect));
        assertTrue(redirect.getFlashAttributes().containsKey("flashError"));
        assertFalse(model.containsAttribute("selectedLevelId"));
    }
}
