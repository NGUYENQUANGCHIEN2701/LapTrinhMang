package com.tankzors.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tankzors.network.protocol.InputMessage;

/**
 * Parse raw WebSocket JSON thành {@link InputMessage}.
 *
 * <p>Tái hiện logic {@code MessageHandler.parseInput} của server Javalin cũ:
 * chấp nhận cả dạng flat ({@code { type:"INPUT", action:"MOVE", direction:"UP" }})
 * và nested payload ({@code { type:"MOVE_INPUT", payload:{ direction:"UP", moving:true } }}).
 *
 * <p>Flat hoá {@code payload.*} lên top-level để Gson bind vào
 * {@link InputMessage}.</p>
 */
public class InputPacketParser {

    private final Gson gson;

    public InputPacketParser(Gson gson) {
        this.gson = gson;
    }

    public InputMessage parse(String raw) {
        JsonObject packet = JsonParser.parseString(raw).getAsJsonObject();
        JsonObject normalized = packet.deepCopy();

        if (packet.has("payload") && packet.get("payload").isJsonObject()) {
            JsonObject payload = packet.getAsJsonObject("payload");
            for (String key : new String[]{"action", "direction", "moving", "playerName", "levelId", "mode"}) {
                if (!normalized.has(key) && payload.has(key)) {
                    normalized.add(key, payload.get(key));
                }
            }
        }

        InputMessage input = gson.fromJson(normalized, InputMessage.class);

        // Nếu type là JOIN_GAME/MOVE_INPUT/SHOOT_INPUT -> dùng làm action
        // (giống MessageHandler.handleInputMessage cũ)
        if (input.getAction() == null && input.getType() != null) {
            input.setAction(input.getType());
        }
        return input;
    }
}
