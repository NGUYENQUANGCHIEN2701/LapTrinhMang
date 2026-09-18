package com.tankzors.web;

import com.tankzors.application.PlayerProgressService;
import com.tankzors.identity.PlayerIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Trang lobby co-op (Bước E). MVP: tạo phòng / nhập mã tham gia / ready / start.
 * Giao tiếp realtime qua WebSocket endpoint /coop.
 */
@Controller
@RequestMapping("/coop")
public class CoopLobbyController {

    private final PlayerIdentityResolver identity;
    private final PlayerProgressService progress;

    public CoopLobbyController(PlayerIdentityResolver identity, PlayerProgressService progress) {
        this.identity = identity;
        this.progress = progress;
    }

    @GetMapping
    public String lobby(@RequestParam(name = "room", required = false) String room,
                        @RequestParam(name = "levelId", defaultValue = "campaign_1") String levelId,
                        HttpServletRequest request, Model model) {
        String playerId = identity.resolve(request);
        List<String> unlocked = progress.getUnlocked(playerId);
        model.addAttribute("playerId", playerId);
        model.addAttribute("roomCode", room == null ? "" : room);
        model.addAttribute("levelId", levelId);
        model.addAttribute("unlockedLevels", unlocked);
        model.addAttribute("wsUrl", deriveWsUrl(request));
        return "coop";
    }

    private static String deriveWsUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String ws = "https".equals(scheme) ? "wss" : "ws";
        String host = request.getServerName();
        int port = request.getServerPort();
        String context = request.getContextPath();
        return ws + "://" + host + ":" + port + context + "/coop";
    }
}
