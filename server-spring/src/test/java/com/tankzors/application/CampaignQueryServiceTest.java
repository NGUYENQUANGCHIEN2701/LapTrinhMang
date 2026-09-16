package com.tankzors.application;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test cho campaign metadata service (plan section 11:
 * "Campaign metadata service trả đúng 3 level").
 */
class CampaignQueryServiceTest {

    private final CampaignQueryService service = new StaticCampaignQueryService();

    @Test
    void getAvailableLevelsReturnsExactlyThreeLevelsInOrder() {
        List<CampaignLevelView> levels = service.getAvailableLevels();
        assertEquals(3, levels.size());
        assertEquals("campaign_1", levels.get(0).getId());
        assertEquals("campaign_2", levels.get(1).getId());
        assertEquals("campaign_3", levels.get(2).getId());
    }

    @Test
    void getLevelReturnsMetadataForKnownLevel() {
        CampaignLevelView level = service.getLevel("campaign_2").orElseThrow();
        assertEquals("campaign_2", level.getId());
        assertEquals("Màn 2 - Chiến hào", level.getName());
        assertEquals("MEDIUM", level.getDifficulty());
    }

    @Test
    void getLevelIsCaseInsensitiveAndTrims() {
        assertTrue(service.getLevel(" CAMPAIGN_3 ").isPresent());
    }

    @Test
    void getLevelReturnsEmptyForUnknownOrNull() {
        assertTrue(service.getLevel("campaign_99").isEmpty());
        assertTrue(service.getLevel(null).isEmpty());
        assertTrue(service.getLevel("  ").isEmpty());
    }
}
