package com.tankzors.network.protocol;

/**
 * Message gửi từ server về client thông báo kết thúc trận đấu.
 *
 * <p>Ví dụ:
 * <pre>{@code
 *   {"type": "RESULT", "winner": "player1", "stars": 3, "reward": 100, "timestamp": 1234567890}
 * }</pre>
 * </p>
 */
public class ResultMessage extends GameMessage {

    private String winner;
    private int stars;
    private int reward;
    private int score;

    public ResultMessage() {
        super("RESULT");
    }

    public ResultMessage(String winner, int stars, int reward, int score) {
        super("RESULT");
        this.winner = winner;
        this.stars = stars;
        this.reward = reward;
        this.score = score;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public int getReward() {
        return reward;
    }

    public void setReward(int reward) {
        this.reward = reward;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
