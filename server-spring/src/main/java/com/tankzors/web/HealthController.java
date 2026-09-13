package com.tankzors.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints tương đương Javalin cũ:
 *
 * <ul>
 *   <li>{@code GET /} -> "Tankzors Server is running" (text).</li>
 *   <li>{@code GET /api/health} -> JSON {@code {"status":"UP","port":8081}}.</li>
 * </ul>
 *
 * <p>Phase 1 chỉ để smoke test Spring Boot start. Phase 2 thêm list-map
 * endpoint nếu cần.</p>
 */
@RestController
public class HealthController {

    @GetMapping("/")
    public String root() {
        return "Tankzors Server (Spring) is running";
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "port", 8081);
    }
}
