package com.tankzors.client.ui;

import com.tankzors.client.gameplay.map.MapLoader;
import com.tankzors.client.shop.PlayerShopState;
import com.tankzors.client.shop.ResearchTech;
import com.tankzors.client.shop.ShopItem;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class VietnameseUiTextTest {
    @Test void allUiFontsCoverVietnameseMissionAndInventoryText() throws Exception {
        var strings = new ArrayList<String>();
        strings.add("NHIỆM VỤ — Nhấn Enter để bắt đầu");
        PlayerShopState shop = new PlayerShopState();
        for (ShopItem item : ShopItem.values()) {
            strings.add(item.getDisplayName());
            strings.add(shop.getItemDescription(item));
            strings.add(shop.getInventoryUsage(item));
        }
        for (ResearchTech tech : ResearchTech.values()) {
            strings.add(tech.getDisplayName());
            strings.add(tech.getDescription());
        }
        for (int level = 2; level <= 3; level++) {
            var mission = MapLoader.parse(Files.readString(Path.of("../assets/maps/campaign_" + level + ".json"),
                    StandardCharsets.UTF_8)).getMission();
            assertTrue(mission.getBriefing().contains("Nhiệm vụ:"));
            strings.add(mission.getBriefing());
            strings.add(mission.getDebriefing());
            mission.getTriggers().forEach(trigger -> { if (trigger.text != null) strings.add(trigger.text); });
        }
        for (String name : new String[]{"game-font", "vietnamese-font", "shop-font"}) {
            Set<Integer> glyphs = new HashSet<>();
            Matcher matcher = Pattern.compile("(?m)^char id=(\\d+) ").matcher(Files.readString(
                    Path.of("../assets/fonts/" + name + ".fnt"), StandardCharsets.UTF_8));
            while (matcher.find()) glyphs.add(Integer.parseInt(matcher.group(1)));
            for (String text : strings) {
                text.codePoints().filter(code -> code >= 32).forEach(code ->
                        assertTrue(glyphs.contains(code), name + " thiếu ký tự " + code));
            }
        }
    }
}
