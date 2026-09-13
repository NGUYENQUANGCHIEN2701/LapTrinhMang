package com.tankzors.client.desktop;

/**
 * Phase 1 marker class. Phase 3 sẽ là LWJGL3 launcher:
 * {@code Lwjgl3Application(new GameClientConfig(), config)}.
 */
public final class DesktopLauncher {
    private DesktopLauncher() {
    }

    public static String version() {
        return "desktop launcher skeleton (Phase 1)";
    }
}
