package com.tankzors.application;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

/**
 * Implementation tĩnh của {@link CampaignQueryService} dựa trên
 * 3 map campaign hiện có trong {@code resources/maps}.
 *
 * <p>Sau này có thể thay bằng version đọc metadata từ map JSON
 * hoặc database mà không đổi controller/template.</p>
 */
@Service
public class StaticCampaignQueryService implements CampaignQueryService {

    private static final List<CampaignLevelView> LEVELS = List.of(
        new CampaignLevelView("campaign_1", "Màn 1 - Huấn luyện", "EASY", "Dễ", 1,
            "Bản đồ mở rộng, làm quen điều khiển và ngắm bắn."),
        new CampaignLevelView("campaign_2", "Màn 2 - Chiến hào", "MEDIUM", "Trung bình", 2,
            "Chiến hào quanh co và chướng ngại vật chông sắt chống tăng."),
        new CampaignLevelView("campaign_3", "Màn 3 - Pháo đài", "HARD", "Khó", 3,
            "Pháo đài kiên cố, kẻ địch tinh nhuệ bao vây 3 hướng.")
    );

    @Override
    public List<CampaignLevelView> getAvailableLevels() {
        return LEVELS;
    }

    @Override
    public Optional<CampaignLevelView> getLevel(String levelId) {
        if (levelId == null || levelId.isBlank()) {
            return Optional.empty();
        }
        String normalized = levelId.trim().toLowerCase(Locale.ROOT);
        return LEVELS.stream()
            .filter(level -> level.getId().equals(normalized))
            .findFirst();
    }
}
