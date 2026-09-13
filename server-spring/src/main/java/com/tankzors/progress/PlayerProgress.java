package com.tankzors.progress;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Một cặp (sessionId, unlocked_level) — record rằng player đã unlock màn nào.
 *
 * <p>Primary key composite: không có auto id, vì chỉ cần (sid, level) duy nhất.</p>
 */
@Entity
@Table(name = "player_progress")
@IdClass(PlayerProgress.Pk.class)
public class PlayerProgress {

    @Id
    @Column(name = "session_id", length = 80, nullable = false)
    private String sessionId;

    @Id
    @Column(name = "unlocked_level", length = 30, nullable = false)
    private String unlockedLevel;

    public PlayerProgress() {
    }

    public PlayerProgress(String sessionId, String unlockedLevel) {
        this.sessionId = sessionId;
        this.unlockedLevel = unlockedLevel;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getUnlockedLevel() { return unlockedLevel; }
    public void setUnlockedLevel(String unlockedLevel) { this.unlockedLevel = unlockedLevel; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerProgress that)) return false;
        return Objects.equals(sessionId, that.sessionId)
            && Objects.equals(unlockedLevel, that.unlockedLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, unlockedLevel);
    }

    /** Composite primary key (JPA requires static nested class). */
    public static class Pk implements Serializable {
        private String sessionId;
        private String unlockedLevel;

        public Pk() {
        }

        public Pk(String sessionId, String unlockedLevel) {
            this.sessionId = sessionId;
            this.unlockedLevel = unlockedLevel;
        }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getUnlockedLevel() { return unlockedLevel; }
        public void setUnlockedLevel(String unlockedLevel) { this.unlockedLevel = unlockedLevel; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(sessionId, pk.sessionId)
                && Objects.equals(unlockedLevel, pk.unlockedLevel);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sessionId, unlockedLevel);
        }
    }
}
