package com.tankzors.protocol;

/**
 * Packet {@code JOIN_GAME} (client -> server) — v1 mục 3.2.
 *
 * <p>Client React gửi dạng nested payload:
 * {@code { "type":"JOIN_GAME", "payload":{ "mode":"campaign",
 * "levelId":"campaign_1", "playerName":"Player" } }}.
 * Server handler phải flat hoá payload lên top-level trước khi bind
 * (giống {@code MessageHandler.parseInput} cũ) để Gson bind vào class
 * này hoạt động.</p>
 */
public class JoinGamePacket extends BasePacket {
    private String mode;
    private String levelId;
    private String playerName;

    public JoinGamePacket() {
        super(PacketType.JOIN_GAME.wireName());
    }

    public JoinGamePacket(String mode, String levelId, String playerName) {
        super(PacketType.JOIN_GAME.wireName());
        this.mode = mode;
        this.levelId = levelId;
        this.playerName = playerName;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getLevelId() {
        return levelId;
    }

    public void setLevelId(String levelId) {
        this.levelId = levelId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}
