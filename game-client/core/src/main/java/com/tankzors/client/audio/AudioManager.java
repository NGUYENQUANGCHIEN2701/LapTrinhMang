package com.tankzors.client.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;

/**
 * Quản lý nạp và phát toàn bộ âm thanh hiệu ứng (SFX) và nhạc nền (BGM) cho game.
 *
 * <p>Hỗ trợ điều khiển âm lượng, tắt/bật tiếng nhanh (mute toggle), giới hạn tần suất
 * phát (throttle) để tránh clipping âm thanh khi nhiều viên đạn va chạm cùng tick,
 * và graceful fallback khi chạy trong môi trường headless test (Gdx.audio == null).</p>
 */
public class AudioManager implements Disposable {

    private static final String TAG = "AudioManager";

    // Throttle intervals (ms)
    private static final long WALL_HIT_INTERVAL_MS = 50;
    private static final long TANK_HIT_INTERVAL_MS = 50;

    // SFX
    private Sound shootSound;
    private Sound hitWallSound;
    private Sound hitTankSound;
    private Sound explosionMediumSound;
    private Sound explosionLargeSound;
    private Sound uiClickSound;
    private Sound uiWindowSound;
    private Sound uiWeapSwitchSound;

    // Music
    private Music track1;
    private Music track2;
    private Music track3;
    private Music victoryMusic;
    private Music defeatMusic;
    private Music briefingMusic;
    private Music currentMusic;

    // Volumes (0.0f - 1.0f)
    private float soundVolume = 0.8f;
    private float musicVolume = 0.7f;
    private boolean muted = false;

    // Throttling state
    private long lastWallHitTime = 0;
    private long lastTankHitTime = 0;

    public AudioManager() {
    }

    public AudioManager(float initialSoundVolume, float initialMusicVolume) {
        setSoundVolume(initialSoundVolume);
        setMusicVolume(initialMusicVolume);
    }

    private void log(String msg) {
        if (Gdx.app != null) Gdx.app.log(TAG, msg);
    }

    private void error(String msg) {
        if (Gdx.app != null) Gdx.app.error(TAG, msg);
    }

    /**
     * Nạp toàn bộ assets âm thanh từ thư mục assets nội bộ.
     */
    public void loadAll() {
        if (Gdx.audio == null || Gdx.files == null) {
            log("Audio subsystem not available (headless mode)");
            return;
        }

        try {
            shootSound = loadSound("sounds/shell_default.ogg");
            hitWallSound = loadSound("sounds/hit_wall_concrete.ogg");
            hitTankSound = loadSound("sounds/hit_metal01.ogg");
            explosionMediumSound = loadSound("sounds/explosion_medium01.ogg");
            explosionLargeSound = loadSound("sounds/explosion_large.ogg");
            uiClickSound = loadSound("sounds/ui_button_down.ogg");
            uiWindowSound = loadSound("sounds/ui_window.ogg");
            uiWeapSwitchSound = loadSound("sounds/ui_weap_switch.ogg");

            track1 = loadMusic("music/track1.ogg");
            track2 = loadMusic("music/track2.ogg");
            track3 = loadMusic("music/track3.ogg");
            victoryMusic = loadMusic("music/victory.ogg");
            defeatMusic = loadMusic("music/defeat.ogg");
            briefingMusic = loadMusic("music/briefing.ogg");
        } catch (Exception e) {
            error("Lỗi nạp audio: " + e.getMessage());
        }
    }

    private Sound loadSound(String path) {
        try {
            if (Gdx.files != null && Gdx.audio != null && Gdx.files.internal(path).exists()) {
                return Gdx.audio.newSound(Gdx.files.internal(path));
            }
        } catch (Exception e) {
            error("Không thể nạp sound: " + path + " - " + e.getMessage());
        }
        return null;
    }

    private Music loadMusic(String path) {
        try {
            if (Gdx.files != null && Gdx.audio != null && Gdx.files.internal(path).exists()) {
                return Gdx.audio.newMusic(Gdx.files.internal(path));
            }
        } catch (Exception e) {
            error("Không thể nạp music: " + path + " - " + e.getMessage());
        }
        return null;
    }

    // ================= SFX PLAYBACK =================

    public void playShoot(boolean isPlayer) {
        if (muted || shootSound == null) return;
        try {
            float vol = soundVolume * (isPlayer ? 1.0f : 0.75f);
            shootSound.play(clamp01(vol));
        } catch (Exception ignored) {}
    }

    public void playHitWall() {
        if (muted || hitWallSound == null) return;
        long now = System.currentTimeMillis();
        if (now - lastWallHitTime < WALL_HIT_INTERVAL_MS) return;
        lastWallHitTime = now;
        try {
            hitWallSound.play(clamp01(soundVolume * 0.7f));
        } catch (Exception ignored) {}
    }

    public void playHitTank(boolean isPlayer) {
        if (muted || hitTankSound == null) return;
        long now = System.currentTimeMillis();
        if (now - lastTankHitTime < TANK_HIT_INTERVAL_MS) return;
        lastTankHitTime = now;
        try {
            float vol = soundVolume * (isPlayer ? 1.0f : 0.85f);
            hitTankSound.play(clamp01(vol));
        } catch (Exception ignored) {}
    }

    public void playTankDestroyed(boolean isPlayer) {
        if (muted) return;
        try {
            if (isPlayer) {
                if (explosionLargeSound != null) {
                    explosionLargeSound.play(clamp01(soundVolume));
                }
            } else {
                if (explosionMediumSound != null) {
                    explosionMediumSound.play(clamp01(soundVolume * 0.9f));
                }
            }
        } catch (Exception ignored) {}
    }

    public void playUiClick() {
        if (muted || uiClickSound == null) return;
        try {
            uiClickSound.play(clamp01(soundVolume));
        } catch (Exception ignored) {}
    }

    // ================= MUSIC PLAYBACK =================

    public void playBattleMusic(String levelId) {
        Music target = track1;
        if ("campaign_2".equalsIgnoreCase(levelId)) {
            target = track2 != null ? track2 : track1;
        } else if ("campaign_3".equalsIgnoreCase(levelId)) {
            target = track3 != null ? track3 : track1;
        }

        switchMusic(target, true);
    }

    public void playVictoryMusic() {
        switchMusic(victoryMusic, false);
    }

    public void playDefeatMusic() {
        switchMusic(defeatMusic, false);
    }

    public void playBriefingMusic() {
        switchMusic(briefingMusic, true);
    }

    public void playUiWindow() {
        if (muted || uiWindowSound == null) return;
        try {
            uiWindowSound.play(clamp01(soundVolume));
        } catch (Exception ignored) {}
    }

    public void playUiWeapSwitch() {
        if (muted || uiWeapSwitchSound == null) return;
        try {
            uiWeapSwitchSound.play(clamp01(soundVolume));
        } catch (Exception ignored) {}
    }
    public void stopMusic() {
        if (currentMusic != null) {
            try {
                currentMusic.stop();
            } catch (Exception ignored) {}
            currentMusic = null;
        }
    }

    private void switchMusic(Music newMusic, boolean looping) {
        if (newMusic == null) return;
        if (currentMusic == newMusic && currentMusic.isPlaying()) return;

        stopMusic();
        currentMusic = newMusic;
        try {
            currentMusic.setLooping(looping);
            currentMusic.setVolume(muted ? 0.0f : clamp01(musicVolume));
            currentMusic.play();
        } catch (Exception ignored) {}
    }

    // ================= CONTROLS & SETTINGS =================

    public void toggleMute() {
        setMuted(!muted);
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (currentMusic != null) {
            try {
                currentMusic.setVolume(muted ? 0.0f : clamp01(musicVolume));
            } catch (Exception ignored) {}
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public void setSoundVolume(float soundVolume) {
        this.soundVolume = clamp01(soundVolume);
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(float musicVolume) {
        this.musicVolume = clamp01(musicVolume);
        if (currentMusic != null && !muted) {
            try {
                currentMusic.setVolume(this.musicVolume);
            } catch (Exception ignored) {}
        }
    }

    private static float clamp01(float v) {
        if (v < 0.0f) return 0.0f;
        if (v > 1.0f) return 1.0f;
        return v;
    }

    @Override
    public void dispose() {
        stopMusic();
        if (shootSound != null) { shootSound.dispose(); shootSound = null; }
        if (hitWallSound != null) { hitWallSound.dispose(); hitWallSound = null; }
        if (hitTankSound != null) { hitTankSound.dispose(); hitTankSound = null; }
        if (explosionMediumSound != null) { explosionMediumSound.dispose(); explosionMediumSound = null; }
        if (explosionLargeSound != null) { explosionLargeSound.dispose(); explosionLargeSound = null; }
        if (uiClickSound != null) { uiClickSound.dispose(); uiClickSound = null; }
        if (uiWindowSound != null) { uiWindowSound.dispose(); uiWindowSound = null; }
        if (uiWeapSwitchSound != null) { uiWeapSwitchSound.dispose(); uiWeapSwitchSound = null; }

        if (track1 != null) { track1.dispose(); track1 = null; }
        if (track2 != null) { track2.dispose(); track2 = null; }
        if (track3 != null) { track3.dispose(); track3 = null; }
        if (victoryMusic != null) { victoryMusic.dispose(); victoryMusic = null; }
        if (defeatMusic != null) { defeatMusic.dispose(); defeatMusic = null; }
        if (briefingMusic != null) { briefingMusic.dispose(); briefingMusic = null; }
    }
}

