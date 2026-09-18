package com.tankzors.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Tiện ích nạp dữ liệu bản đồ từ file JSON.
 *
 * <p>Đọc file JSON từ classpath (ví dụ {@code resources/maps/campaign_1.json})
 * hoặc đường dẫn file hệ thống và parse thành đối tượng {@link GameMap}.</p>
 */
public class MapLoader {

    private static final Logger logger = LoggerFactory.getLogger(MapLoader.class);
    private static final Gson gson = new GsonBuilder().create();

    private MapLoader() {
    }

    /**
     * Nạp bản đồ theo tên (ví dụ "campaign_1" hoặc "campaign_1.json").
     *
     * @param mapName tên bản đồ
     * @return đối tượng GameMap đã được khởi tạo, hoặc null nếu không tìm thấy
     */
    public static GameMap load(String mapName) {
        if (mapName == null || mapName.trim().isEmpty()) {
            logger.error("[MapLoader] Tên bản đồ không được để trống!");
            return null;
        }

        String fileName = mapName.endsWith(".json") ? mapName : mapName + ".json";
        String resourcePath = "maps/" + fileName;

        // 1. Thử đọc từ ClassLoader (Classpath resources)
        InputStream is = MapLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            // Thử thêm dấu gạch chéo đầu
            is = MapLoader.class.getResourceAsStream("/" + resourcePath);
        }

        // 2. Nếu không có trong classpath (ví dụ khi chạy test hoặc đường dẫn file trực tiếp)
        if (is == null) {
            File file = new File(resourcePath);
            if (!file.exists()) {
                file = new File("src/main/resources/" + resourcePath);
            }
            if (file.exists()) {
                try {
                    is = new FileInputStream(file);
                } catch (Exception e) {
                    logger.warn("[MapLoader] Không thể mở file {}: {}", file.getPath(), e.getMessage());
                }
            }
        }

        if (is == null) {
            logger.error("[MapLoader] Không tìm thấy file bản đồ: {}", resourcePath);
            return null;
        }

        return loadFromStream(is);
    }

    /**
     * Nạp bản đồ từ một {@link InputStream}.
     *
     * @param is luồng dữ liệu JSON
     * @return đối tượng GameMap đã được khởi tạo, hoặc null nếu lỗi
     */
    public static GameMap loadFromStream(InputStream is) {
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            GameMap map = gson.fromJson(reader, GameMap.class);
            if (map != null) {
                map.initializeMap();
                logger.info("🗺️ [MapLoader] Nạp bản đồ thành công: '{}' ({}x{} ô, {} tường, {} spawn points)",
                        map.getName(), map.getWidth(), map.getHeight(),
                        map.getWalls().size(), map.getSpawnPoints().size());
            }
            return map;
        } catch (Exception e) {
            logger.error("❌ [MapLoader] Lỗi phân tích cú pháp JSON bản đồ: {}", e.getMessage(), e);
            return null;
        }
    }
}
