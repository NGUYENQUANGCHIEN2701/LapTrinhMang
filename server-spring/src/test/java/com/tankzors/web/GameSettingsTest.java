package com.tankzors.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test cho validate/clamp của settings (plan section 11).
 */
class GameSettingsTest {

    @Test
    void clampPercentReturnsValueWithinZeroToHundred() {
        assertEquals(0, GameSettings.clampPercent(-5));
        assertEquals(0, GameSettings.clampPercent(0));
        assertEquals(55, GameSettings.clampPercent(55));
        assertEquals(100, GameSettings.clampPercent(100));
        assertEquals(100, GameSettings.clampPercent(150));
    }

    @Test
    void clampScaleReturnsValueWithinFiftyToTwoHundred() {
        assertEquals(50, GameSettings.clampScale(10));
        assertEquals(50, GameSettings.clampScale(50));
        assertEquals(100, GameSettings.clampScale(100));
        assertEquals(200, GameSettings.clampScale(200));
        assertEquals(200, GameSettings.clampScale(999));
    }

    @Test
    void normalizeLanguageFallsBackToVietnamese() {
        assertEquals("vi", GameSettings.normalizeLanguage(null));
        assertEquals("vi", GameSettings.normalizeLanguage(""));
        assertEquals("vi", GameSettings.normalizeLanguage("fr"));
        assertEquals("en", GameSettings.normalizeLanguage("EN"));
        assertEquals("vi", GameSettings.normalizeLanguage(" vi "));
    }

    @Test
    void fromRequestWithNullsUsesDefaults() {
        GameSettings settings = GameSettings.fromRequest(null, null, null, null, null);
        assertEquals(GameSettings.DEFAULT_MUSIC_VOLUME, settings.getMusicVolume());
        assertEquals(GameSettings.DEFAULT_SOUND_VOLUME, settings.getSoundVolume());
        assertEquals(GameSettings.DEFAULT_LANGUAGE, settings.getLanguage());
        assertFalse(settings.isShowFps());
        assertEquals(GameSettings.DEFAULT_GAME_SCALE, settings.getGameScale());
    }

    @Test
    void fromRequestClampsOutOfRangeValues() {
        GameSettings settings = GameSettings.fromRequest(150, -5, "fr", true, 999);
        assertEquals(100, settings.getMusicVolume());
        assertEquals(0, settings.getSoundVolume());
        assertEquals("vi", settings.getLanguage());
        assertTrue(settings.isShowFps());
        assertEquals(200, settings.getGameScale());
    }

    @Test
    void parseIntOrNullHandlesBadInput() {
        assertEquals(80, GameSettings.parseIntOrNull("80"));
        assertEquals(80, GameSettings.parseIntOrNull(" 80 "));
        assertNull(GameSettings.parseIntOrNull(null));
        assertNull(GameSettings.parseIntOrNull(""));
        assertNull(GameSettings.parseIntOrNull("abc"));
    }
}
