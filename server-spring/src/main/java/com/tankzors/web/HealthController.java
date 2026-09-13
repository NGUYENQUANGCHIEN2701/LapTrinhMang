package com.tankzors.web;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST health endpoint.
 *
 * <p>Phase 4.5: route {@code /} đã chuyển sang {@link PageController#home}
 * để hiển thị Thymeleaf template. Controller này chỉ giữ lại
 * {@code GET /api/health}.</p>
 */
@RestController
public class HealthController {

    @Value("${server.port:8081}")
    private int serverPort;

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "port", serverPort);
    }
}
