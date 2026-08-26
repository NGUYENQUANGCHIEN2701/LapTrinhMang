package com.tankzors.network.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * Message gửi từ server về client chứa trạng thái toàn bộ trận đấu (vị trí xe tăng, đạn, điểm số).
 *
 * <p>Ví dụ:
 * <pre>{@code
 *   {"type": "STATE", "tanks": [...], "bullets": [...], "score": 100, "gameStatus": "PLAYING", "timestamp": 1234567890}
 * }</pre>
 * </p>
 */
public class StateMessage extends GameMessage {

    private List<Object> tanks;
    private List<Object> bullets;
    private int score;
    private String gameStatus; // "PLAYING", "VICTORY", "DEFEAT", "WAITING"

    public StateMessage() {
        super("STATE");
        this.tanks = new ArrayList<>();
        this.bullets = new ArrayList<>();
        this.gameStatus = "PLAYING";
    }

    public List<Object> getTanks() {
        return tanks;
    }

    public void setTanks(List<Object> tanks) {
        this.tanks = tanks;
    }

    public List<Object> getBullets() {
        return bullets;
    }

    public void setBullets(List<Object> bullets) {
        this.bullets = bullets;
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
