package com.tankzors.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tankzors.room.Room;
import com.tankzors.room.RoomRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler relay co-op v2 (endpoint /coop). Server KHÔNG chạy simulation — chỉ
 * forward input guest->host và WORLD_STATE/MATCH_RESULT host->guest + persistence.
 *
 * <p>Envelope: { version, type, roomId, runId, seq, payload }. Server gắn
 * senderId từ connection; không tin senderId client truyền.</p>
 *
 * <p>Quyền: chỉ host gửi WORLD_STATE / MATCH_RESULT / START_MATCH.
 * Chỉ thành viên phòng được forward. Cấm gửi sang phòng khác.</p>
 */
@Component
public class RoomRelayHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RoomRelayHandler.class);
    private static final ObjectMapper M = new ObjectMapper();

    private static final String ATTR_PLAYER = "tankzors.playerId";
    private static final String ATTR_ROOM = "tankzors.roomId";

    private final RoomRegistry registry;

    public RoomRelayHandler(RoomRegistry registry) {
        this.registry = registry;
        // identity resolve qua RelayHandshakeInterceptor (HTTP session); handler chỉ đọc attributes.
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        SessionHolder.put(session);
        String playerId = (String) session.getAttributes().get(ATTR_PLAYER);
        String connectionId = session.getId();
        log.info("[Relay] connect conn={} player={}", connectionId, playerId);
        // Gửi CONNECT.
        send(session, envelope("v2", "CONNECT", null, null, 0,
                Map.of("connectionId", connectionId, "playerId", playerId == null ? "" : playerId, "protocolVersion", "v2")));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        if (payload.length() > 1024 * 64) {
            sendError(session, "PACKET_TOO_LARGE", "Gói vượt 64KB");
            return;
        }
        JsonNode root;
        try { root = M.readTree(payload); }
        catch (Exception e) {
            sendError(session, "BAD_JSON", "Envelope không parse được");
            return;
        }
        String type = txt(root, "type");
        String version = txt(root, "version");
        if (!"v2".equalsIgnoreCase(version)) {
            sendError(session, "UNSUPPORTED_VERSION", "Chỉ hỗ trợ protocol v2");
            return;
        }
        String connectionId = session.getId();
        Room room = registry.findByConnection(connectionId).orElse(null);
        String roomId = txt(root, "roomId");

        switch (type == null ? "" : type.toUpperCase()) {
            case "CREATE_ROOM" -> handleCreate(session, root);
            case "JOIN_ROOM" -> handleJoin(session, root);
            case "LEAVE_ROOM" -> handleLeave(session);
            case "READY" -> handleReady(session, room, root);
            case "START_MATCH" -> handleStart(session, room, root);
            case "PLAYER_INPUT" -> forwardToHost(session, room, root, type);
            case "WORLD_STATE", "MATCH_RESULT", "PAUSE_MATCH", "RESUME_MATCH" -> forwardToGuests(session, room, root, type);
            case "PING" -> send(session, envelope("v2", "PONG", null, null, 0, Map.of()));
            default -> sendError(session, "UNKNOWN_TYPE", "Loại gói không xác định: " + type);
        }
    }

    private void handleCreate(WebSocketSession session, JsonNode root) {
        String playerId = playerIdOf(session);
        Room room = registry.create(playerId);
        if (!room.addMember(new Room.Member(session.getId(), playerId, true))) {
            sendError(session, "ROOM_FULL", "Không thể thêm host");
            return;
        }
        registry.bindConnection(session.getId(), room);
        session.getAttributes().put(ATTR_ROOM, room.getRoomId());
        log.info("[Relay] CREATE room={} host={}", room.getRoomId(), playerId);
        broadcastRoomState(room);
    }

    private void handleJoin(WebSocketSession session, JsonNode root) {
        String roomId = txt(root, "roomId");
        Room room = registry.get(roomId);
        if (room == null) {
            sendError(session, "ROOM_NOT_FOUND", "Phòng không tồn tại: " + roomId);
            return;
        }
        if (room.getStatus() != Room.Status.LOBBY) {
            sendError(session, "ROOM_NOT_JOINABLE", "Phòng đang " + room.getStatus());
            return;
        }
        String playerId = playerIdOf(session);
        if (!room.addMember(new Room.Member(session.getId(), playerId, false))) {
            sendError(session, "ROOM_FULL", "Phòng đã đủ 2 người");
            return;
        }
        registry.bindConnection(session.getId(), room);
        session.getAttributes().put(ATTR_ROOM, room.getRoomId());
        log.info("[Relay] JOIN room={} player={}", room.getRoomId(), playerId);
        broadcastRoomState(room);
    }

    private void handleLeave(WebSocketSession session) {
        disconnectFromRoom(session, "PLAYER_LEFT");
    }

    private void handleReady(WebSocketSession session, Room room, JsonNode root) {
        if (room == null) { sendError(session, "NOT_IN_ROOM", "Chưa tham gia phòng"); return; }
        Room.Member m = room.member(session.getId());
        if (m == null) return;
        JsonNode p = root.get("payload");
        boolean ready = p != null && p.has("ready") && p.get("ready").asBoolean();
        m.ready = ready;
        if (p != null && p.has("levelId") && room.getHostPlayerId().equals(m.playerId)) {
            room.setLevelId(p.get("levelId").asText());
        }
        broadcastRoomState(room);
    }

    private void handleStart(WebSocketSession session, Room room, JsonNode root) {
        if (room == null) { sendError(session, "NOT_IN_ROOM", null); return; }
        Room.Member m = room.member(session.getId());
        if (m == null || !m.host) {
            sendError(session, "NOT_HOST", "Chỉ host được START_MATCH");
            return;
        }
        if (!room.allReady()) {
            sendError(session, "NOT_READY", "Chưa đủ ready");
            return;
        }
        if (room.getLevelId() == null) {
            sendError(session, "NO_LEVEL", "Chưa chọn level");
            return;
        }
        String runId = "coop-" + room.getRoomId() + "-" + System.currentTimeMillis();
        room.setRunId(runId);
        room.setStatus(Room.Status.PLAYING);
        ObjectNode startEnvelope = envelope("v2", "MATCH_STARTED", room.getRoomId(), runId, 0,
                Map.of("levelId", room.getLevelId(), "mapVersion", "v1", "runId", runId));
        for (Room.Member mem : room.members()) {
            WebSocketSession s = memberSession(session, mem.connectionId);
            if (s != null && s.isOpen()) send(s, startEnvelope);
        }
        log.info("[Relay] START room={} runId={}", room.getRoomId(), runId);
    }

    private void forwardToHost(WebSocketSession session, Room room, JsonNode root, String type) {
        if (room == null) { sendError(session, "NOT_IN_ROOM", null); return; }
        if (room.getStatus() != Room.Status.PLAYING) {
            sendError(session, "NOT_PLAYING", "Phòng chưa vào trận");
            return;
        }
        // Validate seq (numeric) nếu có — bỏ input cũ/trùng do host xử lý.
        Room.Member sender = room.member(session.getId());
        if (sender == null || sender.host) {
            // Host không cần tự forward input tới chính nó.
            return;
        }
        Room.Member host = room.host();
        if (host == null) return;
        WebSocketSession hostSession = memberSession(session, host.connectionId);
        if (hostSession != null && hostSession.isOpen()) {
            ObjectNode fwd = envelope("v2", type, room.getRoomId(), room.getRunId(),
                    lng(root, "seq"), root.get("payload"));
            ((ObjectNode) fwd).put("senderId", sender.playerId);
            send(hostSession, fwd);
        }
    }

    private void forwardToGuests(WebSocketSession session, Room room, JsonNode root, String type) {
        if (room == null) { sendError(session, "NOT_IN_ROOM", null); return; }
        Room.Member sender = room.member(session.getId());
        if (sender == null || !sender.host) {
            sendError(session, "NOT_HOST", "Chỉ host được gửi " + type);
            return;
        }
        ObjectNode fwd = envelope("v2", type, room.getRoomId(), room.getRunId(),
                lng(root, "seq"), root.get("payload"));
        for (Room.Member mem : room.members()) {
            if (mem.host) continue;
            WebSocketSession s = memberSession(session, mem.connectionId);
            if (s != null && s.isOpen()) send(s, fwd);
        }
    }

    private void broadcastRoomState(Room room) {
        List<Map<String, Object>> members = room.members().stream()
                .map(m -> {
                    Map<String, Object> em = new HashMap<>();
                    em.put("playerId", m.playerId);
                    em.put("host", m.host);
                    em.put("ready", m.ready);
                    return em;
                }).toList();
        ObjectNode env = envelope("v2", "ROOM_STATE", room.getRoomId(), null, 0,
                Map.of("status", room.getStatus().name(), "levelId",
                        room.getLevelId() == null ? "" : room.getLevelId(), "members", members));
        for (Room.Member m : room.members()) {
            WebSocketSession s = memberSession(null, m.connectionId);
            if (s != null && s.isOpen()) send(s, env);
        }
    }

    private void disconnectFromRoom(WebSocketSession session, String reason) {
        Room room = registry.unbindConnection(session.getId());
        if (room != null) {
            room.setCloseReason(reason);
            // Nếu host rời -> đóng phòng.
            Room.Member wasHost = room.host();
            if (wasHost == null || reason.equals("HOST_DISCONNECTED")) {
                room.setStatus(Room.Status.CLOSED);
                room.setCloseReason("HOST_DISCONNECTED");
                ObjectNode closed = envelope("v2", "ROOM_CLOSED", room.getRoomId(), null, 0,
                        Map.of("reason", "HOST_DISCONNECTED"));
                for (Room.Member m : room.members()) {
                    WebSocketSession s = memberSession(session, m.connectionId);
                    if (s != null && s.isOpen()) send(s, closed);
                }
            } else {
                broadcastRoomState(room);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        disconnectFromRoom(session, "DISCONNECTED");
        SessionHolder.onClosed(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("[Relay] transport error conn={}: {}", session.getId(), exception.getMessage());
        disconnectFromRoom(session, "TRANSPORT_ERROR");
    }

    // ===== helpers =====

    private static String txt(JsonNode n, String k) {
        if (n == null) return null;
        JsonNode v = n.get(k);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static long lng(JsonNode n, String k) {
        if (n == null) return 0;
        JsonNode v = n.get(k);
        return v == null || v.isNull() ? 0 : v.asLong();
    }

    private String playerIdOf(WebSocketSession s) {
        Object p = s.getAttributes().get(ATTR_PLAYER);
        return p == null ? "anon" : p.toString();
    }

    private WebSocketSession memberSession(WebSocketSession hint, String connectionId) {
        // Trong handler này ta không lưu all sessions; relay cần sesssion registry ws.
        // Spring quản lý session qua attribute set ở HandshakeInterceptor; ta phải
        // tra cứu qua một ánh xạ ngoài. Đơn giản: HandshakeInterceptor lưu map.
        return SessionHolder.get(connectionId);
    }

    private static ObjectNode envelope(String version, String type, String roomId, String runId, long seq, Object payload) {
        ObjectNode env = M.createObjectNode();
        env.put("version", version);
        env.put("type", type);
        if (roomId != null) env.put("roomId", roomId);
        if (runId != null) env.put("runId", runId);
        env.put("seq", seq);
        if (payload != null) env.set("payload", M.valueToTree(payload));
        return env;
    }

    private void send(WebSocketSession s, ObjectNode env) {
        try { if (s != null && s.isOpen()) s.sendMessage(new TextMessage(M.writeValueAsString(env))); }
        catch (IOException e) { log.warn("[Relay] send failed conn={}", s == null ? "?" : s.getId()); }
    }

    private void sendError(WebSocketSession s, String code, String msg) {
        Map<String, Object> p = new HashMap<>();
        p.put("code", code);
        if (msg != null) p.put("message", msg);
        send(s, envelope("v2", "ERROR", null, null, 0, p));
    }
}
