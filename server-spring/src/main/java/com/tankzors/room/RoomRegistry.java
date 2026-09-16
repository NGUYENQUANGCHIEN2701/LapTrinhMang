package com.tankzors.room;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Registry phòng co-op. Chỉ giữ metadata, KHÔNG chứa GameWorld (host sở hữu). */
@Component
public class RoomRegistry {

    private final Map<String, Room> roomsById = new ConcurrentHashMap<>();
    private final Map<String, Room> roomByConnection = new ConcurrentHashMap<>();

    public Room create(String hostPlayerId) {
        String id = generateRoomId();
        Room room = new Room(id, hostPlayerId);
        roomsById.put(id, room);
        return room;
    }

    public Room get(String roomId) { return roomId == null ? null : roomsById.get(roomId); }

    public Optional<Room> findByConnection(String connectionId) {
        return Optional.ofNullable(connectionId == null ? null : roomByConnection.get(connectionId));
    }

    public void bindConnection(String connectionId, Room room) {
        roomByConnection.put(connectionId, room);
    }

    public Room unbindConnection(String connectionId) {
        Room removed = roomByConnection.remove(connectionId);
        if (removed != null) {
            removed.removeMember(connectionId);
            if (removed.size() == 0) {
                removed.setStatus(Room.Status.CLOSED);
                roomsById.remove(removed.getRoomId());
            }
        }
        return removed;
    }

    private static String generateRoomId() {
        // 6 ký tự; không dùng Math.random ở lớp util để dù test lặp vẫn OK.
        long n = System.nanoTime() & 0xFFFFFFL;
        StringBuilder sb = new StringBuilder();
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        for (int i = 0; i < 6; i++) {
            sb.append(alphabet.charAt((int) (n % alphabet.length())));
            n /= alphabet.length();
        }
        return sb.toString();
    }
}
