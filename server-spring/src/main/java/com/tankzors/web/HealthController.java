package com.tankzors.web;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints tương đương Javalin cũ:
 *
 * <ul>
 *   <li>{@code GET /} -> "Tankzors Server is running" (text).</li>
 *   <li>{@code GET /api/health} -> JSON {@code {"status":"UP","port":8081}}.</li>
 * </ul>
 */
@RestController
public class HealthController {

    @Value("${server.port:8081}")
    private int serverPort;

    @GetMapping("/")
    public String root() {
        return "Tankzors Server (Spring) is running";
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "port", serverPort);
    }
}
