package com.tankzors.map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/** Loads campaign map data without creating server-side gameplay objects. */
public final class MapLoader {

    private static final Logger logger = LoggerFactory.getLogger(MapLoader.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private MapLoader() {}

    public static GameMap load(String mapName) {
        if (mapName == null || mapName.isBlank()) {
            logger.error("[MapLoader] Map name must not be blank");
            return null;
        }

        String fileName = mapName.endsWith(".json") ? mapName : mapName + ".json";
        String resourcePath = "maps/" + fileName;
        InputStream input = MapLoader.class.getClassLoader().getResourceAsStream(resourcePath);

        if (input == null) {
            File file = new File(resourcePath);
            if (!file.exists()) file = new File("src/main/resources/" + resourcePath);
            if (file.exists()) {
                try {
                    input = new FileInputStream(file);
                } catch (Exception exception) {
                    logger.warn("[MapLoader] Cannot open {}: {}", file.getPath(), exception.getMessage());
                }
            }
        }

        if (input == null) {
            logger.error("[MapLoader] Map resource not found: {}", resourcePath);
            return null;
        }
        return loadFromStream(input);
    }

    public static GameMap loadFromStream(InputStream input) {
        try (InputStream stream = input) {
            GameMap map = objectMapper.readValue(stream, GameMap.class);
            logger.info("[MapLoader] Loaded '{}' ({}x{}, {} enemies)",
                    map.getName(), map.getWidth(), map.getHeight(), map.getEnemies().size());
            return map;
        } catch (Exception exception) {
            logger.error("[MapLoader] Invalid map JSON: {}", exception.getMessage());
            return null;
        }
    }
}
