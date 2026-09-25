package com.tankzors.application;

import java.util.List;
import java.util.Optional;

/** Read-only campaign metadata used by the website and bootstrap API. */
public interface CampaignQueryService {
    List<CampaignLevelView> getAvailableLevels();
    Optional<CampaignLevelView> getLevel(String levelId);
}
