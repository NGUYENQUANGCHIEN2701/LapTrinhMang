package com.tankzors.protocol;

/**
 * Packet {@code STATE} (server -> client) — v1 mục 3.5.
 *
 * <p>Server phát 30Hz khi status != WAITING. Daily broadcast gồm danh sách
 * tanks, bullets, map snapshot, score, gameStatus. Tank máu  = 0 được
 * broadcast lần cuối với {@code alive=false} trước khi bị removal tick
 * kế tiếp.</p>
 *
 * <p>Lưu ý về kiểu field: dùng {@code Object} cho TankDto/BulletDto vì v1
 * delegate serialisation sang Gson của server. Phase 2 có thể thay bằng
 * concrete DTO class khi engine/domain đã tách.</p>
 */
public class StatePacket extends BasePacket {
    private java.util.List<Object> tanks;
    private java.util.List<Object> bullets;
    private java.util.Map<String, Object> map;
    private int score;
    private String gameStatus;

    public StatePacket() {
        super(PacketType.STATE.wireName());
        this.tanks = new java.util.ArrayList<>();
        this.bullets = new java.util.ArrayList<>();
        this.gameStatus = GameStatusDto.PLAYING.name();
    }

    public java.util.List<Object> getTanks() {
        return tanks;
    }

    public void setTanks(java.util.List<Object> tanks) {
        this.tanks = tanks;
    }

    public java.util.List<Object> getBullets() {
        return bullets;
    }

    public void setBullets(java.util.List<Object> bullets) {
        this.bullets = bullets;
    }

    public java.util.Map<String, Object> getMap() {
        return map;
    }

    public void setMap(java.util.Map<String, Object> map) {
        this.map = map;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(String gameStatus) {
        this.gameStatus = gameStatus;
    }
}
