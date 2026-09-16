package com.tankzors.web;

import java.util.Locale;
import java.util.Set;

/**
 * Settings game tối thiểu lưu trong HttpSession (Phase 4.5 -
 * chưa cần bảng database settings).
 *
 * <p>Mọi giá trị số được clamp về range hợp lệ qua
 * {@link #fromRequest(Integer, Integer, String, Boolean, Integer)}.</p>
 */
public class GameSettings {

    public static final int MIN_VOLUME = 0;
    public static final int MAX_VOLUME = 100;
    public static final int MIN_SCALE = 50;
    public static final int MAX_SCALE = 200;

    public static final int DEFAULT_MUSIC_VOLUME = 70;
    public static final int DEFAULT_SOUND_VOLUME = 80;
    public static final int DEFAULT_GAME_SCALE = 100;
    public static final String DEFAULT_LANGUAGE = "vi";

    public static final Set<String> SUPPORTED_LANGUAGES = Set.of("vi", "en");

    private int musicVolume = DEFAULT_MUSIC_VOLUME;
    private int soundVolume = DEFAULT_SOUND_VOLUME;
    private String language = DEFAULT_LANGUAGE;
    private boolean showFps = false;
    private int gameScale = DEFAULT_GAME_SCALE;

    public static GameSettings defaults() {
        return new GameSettings();
    }

    /**
     * Build settings từ request params (nullable) sau khi validate + clamp.
     */
    public static GameSettings fromRequest(Integer musicVolume, Integer soundVolume,
                                           String language, Boolean showFps, Integer gameScale) {
        GameSettings settings = new GameSettings();
        settings.setMusicVolume(clampPercent(musicVolume == null ? DEFAULT_MUSIC_VOLUME : musicVolume));
        settings.setSoundVolume(clampPercent(soundVolume == null ? DEFAULT_SOUND_VOLUME : soundVolume));
        settings.setLanguage(normalizeLanguage(language));
        settings.setShowFps(Boolean.TRUE.equals(showFps));
        settings.setGameScale(clampScale(gameScale == null ? DEFAULT_GAME_SCALE : gameScale));
        return settings;
    }

    public static int clampPercent(int value) {
        return Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, value));
    }

    public static int clampScale(int value) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
    }

    public static String normalizeLanguage(String value) {
        if (value == null) {
            return DEFAULT_LANGUAGE;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return SUPPORTED_LANGUAGES.contains(normalized) ? normalized : DEFAULT_LANGUAGE;
    }

    /** Parse số từ form param; trả null nếu không phải số hợp lệ (fallback về default). */
    public static Integer parseIntOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public int getMusicVolume() { return musicVolume; }
    public void setMusicVolume(int musicVolume) {
        this.musicVolume = clampPercent(musicVolume);
    }

    public int getSoundVolume() { return soundVolume; }
    public void setSoundVolume(int soundVolume) {
        this.soundVolume = clampPercent(soundVolume);
    }

    public String getLanguage() { return language; }
    public void setLanguage(String language) {
        this.language = normalizeLanguage(language);
    }

    public boolean isShowFps() { return showFps; }
    public void setShowFps(boolean showFps) {
        this.showFps = showFps;
    }

    public int getGameScale() { return gameScale; }
    public void setGameScale(int gameScale) {
        this.gameScale = clampScale(gameScale);
    }
}
