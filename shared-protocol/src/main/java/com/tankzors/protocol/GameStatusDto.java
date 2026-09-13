package com.tankzors.protocol;

/**
 * Trạng thái trận đấu trên wire (v1).
 *
 * <p>Nguồn: {@code GameState.Status} của server cũ, được chốt tại
 * protocol-v1 mục 3.5.</p>
 */
public enum GameStatusDto {
    WAITING,
    PLAYING,
    VICTORY,
    DEFEAT;

    public static GameStatusDto fromName(String name) {
        if (name == null) return null;
        try {
            return GameStatusDto.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
