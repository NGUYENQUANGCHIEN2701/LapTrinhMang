package com.tankzors.identity;

/** Authenticated identity contains the complete account name after user:. */
public final class PlayerNames {
    private PlayerNames() {}

    public static String accountName(String playerId) {
        return playerId != null && playerId.startsWith("user:") && playerId.length() > 5
                ? playerId.substring(5) : null;
    }

    public static boolean isLegacyGeneratedName(String name) {
        return name != null && name.startsWith("Player-") && name.length() <= 13;
    }
}
