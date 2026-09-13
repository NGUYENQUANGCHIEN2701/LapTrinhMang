package com.tankzors.client;

/**
 * Phase 1 marker class. Phase 3 sẽ là chính entry point LibGDX client:
 * {@code ApplicationListener} render map/tank/bullet từ STATE packet.
 */
public final class GameClient {
    private GameClient() {
    }

    public static String version() {
        return "tankzors-game-client 1.0-SNAPSHOT (Phase 1 skeleton)";
    }
}
