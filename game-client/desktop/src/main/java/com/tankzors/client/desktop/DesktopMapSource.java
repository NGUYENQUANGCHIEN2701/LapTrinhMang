package com.tankzors.client.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.tankzors.client.session.MapSource;

/**
 * Đọc map JSON từ assets {@code maps/<levelId>.json} trên desktop (JVM).
 * Core không được dùng FileHandle trực tiếp (GWT), nên nguồn này ở module desktop.
 *
 * <p>Desktop đọc file đồng bộ nên {@link #requestLevel} chạy callback ngay trong
 * cùng thread call. Cấp runId local (desktop không qua server bootstrap).</p>
 */
public class DesktopMapSource implements MapSource {

    private long runCounter = System.nanoTime();

    @Override
    public String loadMapJson(String levelId) {
        if (levelId == null) return null;
        String path = "maps/" + levelId + ".json";
        FileHandle fh = Gdx.files.internal(path);
        if (!fh.exists()) return null;
        return fh.readString("UTF-8");
    }

    @Override
    public void requestLevel(String levelId, LevelCallback callback) {
        String json = loadMapJson(levelId);
        if (json != null) {
            // runId local + monotonic để mỗi lần restart/next có runId khác nhau.
            String runId = "desktop-" + levelId + "-" + (runCounter++);
            callback.onLevelLoaded(levelId, json, runId, "");
        } else {
            callback.onLoadError("Không tìm thấy map: maps/" + levelId + ".json");
        }
    }
}
