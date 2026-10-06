package com.tankzors.client.render;

/**
 * Pure camera-bound calculations shared by the renderer and JVM tests.
 *
 * <p>The target stays centred while there is map content available on both
 * sides. At a map edge the camera stops, and a map smaller than the viewport
 * is centred instead of being pinned to one corner.</p>
 */
public final class WorldCameraBounds {

    private WorldCameraBounds() {
    }

    public static float centerForAxis(float target, float worldSize, float viewportSize) {
        float safeWorldSize = Math.max(0f, worldSize);
        float safeViewportSize = Math.max(1f, viewportSize);
        if (safeWorldSize <= safeViewportSize) {
            return safeWorldSize / 2f;
        }

        float halfViewport = safeViewportSize / 2f;
        return Math.max(halfViewport, Math.min(target, safeWorldSize - halfViewport));
    }
}
