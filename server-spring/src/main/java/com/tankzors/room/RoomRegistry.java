package com.tankzors.room;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Registry phòng co-op. Chỉ giữ metadata, KHÔNG chứa GameWorld (host sở hữu). */
@Component
public class RoomRegistry {

    private static final String ROOM_ID_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Room> roomsById = new ConcurrentHashMap<>();
    private final Map<String, Room> roomByConnection = new ConcurrentHashMap<>();

    public Room create(String hostPlayerId) {
        while (true) {
            Room room = new Room(generateRoomId(), hostPlayerId);
            if (roomsById.putIfAbsent(room.getRoomId(), room) == null) return room;
        }
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
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(ROOM_ID_ALPHABET.charAt(RANDOM.nextInt(ROOM_ID_ALPHABET.length())));
        }
        return sb.toString();
    }
}
