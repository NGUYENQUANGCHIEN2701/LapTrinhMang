package com.tankzors.client.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.tankzors.client.session.MapSource;

/**
 * Đọc map JSON từ assets {@code maps/<levelId>.json} trên desktop (JVM).
 * Core không được dùng FileHandle trực tiếp (GWT), nên nguồn này ở module desktop.
 */
public class DesktopMapSource implements MapSource {
    @Override
    public String loadMapJson(String levelId) {
        if (levelId == null) return null;
        String path = "maps/" + levelId + ".json";
        FileHandle fh = Gdx.files.internal(path);
        if (!fh.exists()) return null;
        return fh.readString("UTF-8");
    }
}
