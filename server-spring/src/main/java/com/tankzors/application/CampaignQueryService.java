package com.tankzors.application;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction đọc metadata campaign cho UI (Phase 4.5 Thymeleaf shell).
 *
 * <p>Service chỉ được đọc map metadata; không được khởi động
 * {@code GameLoop} hay tạo {@code GameState}.</p>
 */
public interface CampaignQueryService {

    /** Danh sách level campaign khả dụng, theo thứ tự màn chơi. */
    List<CampaignLevelView> getAvailableLevels();

    /** Tra một level theo id (vd {@code campaign_1}); empty nếu không tồn tại. */
    Optional<CampaignLevelView> getLevel(String levelId);
}
