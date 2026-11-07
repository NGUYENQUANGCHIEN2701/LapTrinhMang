package com.tankzors.room;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Một phòng multiplayer co-op metadata. KHÔNG chứa GameWorld/simulation
 * (host client sở hữu mô phỏng). Server chỉ relay.
 *
 * <p>Lifecycle: LOBBY -> PLAYING -> FINISHED -> (xóa hoặc rematch tạo runId mới).
 * Tổng 2 thành viên (MVP). Host có quyền START/WORLD_STATE/MATCH_RESULT.</p>
 */
public final class Room {

    public enum Status { LOBBY, PLAYING, FINISHED, CLOSED }

    public static final class Member {
        public final String connectionId; // WS session id
        public final String playerId;     // identity ứng dụng
        public final boolean host;
        public boolean ready;

        public Member(String connectionId, String playerId, boolean host) {
            this.connectionId = connectionId;
            this.playerId = playerId;
            this.host = host;
            this.ready = false;
        }
    }

    private final String roomId;
    private final String hostPlayerId;
    private final Map<String, Member> members = new LinkedHashMap<>(); // connectionId -> Member
    private Status status = Status.LOBBY;
    private String levelId;
    private String runId;

    public Room(String roomId, String hostPlayerId) {
        this.roomId = roomId;
        this.hostPlayerId = hostPlayerId;
    }

    public String getRoomId() { return roomId; }
    public String getHostPlayerId() { return hostPlayerId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getLevelId() { return levelId; }
    public void setLevelId(String levelId) { this.levelId = levelId; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public synchronized List<Member> members() { return new ArrayList<>(members.values()); }

    public synchronized Member host() {
        for (Member m : members.values()) if (m.host) return m;
        return null;
    }

    public synchronized Member member(String connectionId) { return members.get(connectionId); }

    public synchronized boolean addMember(Member m) {
        if (members.size() >= 2) return false;
        if (members.containsKey(m.connectionId)) return false;
        members.put(m.connectionId, m);
        return true;
    }

    public synchronized Member removeMember(String connectionId) {
        return members.remove(connectionId);
    }

    public synchronized boolean allReady() {
        if (members.size() != 2) return false;
        for (Member m : members.values()) if (!m.ready) return false;
        return true;
    }

    public synchronized int size() { return members.size(); }
}
