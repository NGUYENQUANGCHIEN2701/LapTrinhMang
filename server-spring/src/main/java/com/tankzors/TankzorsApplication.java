package com.tankzors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point Spring Boot target (Phase 1: skeleton chỉ start context +
 * health endpoint, chưa đụng Javalin legacy).
 *
 * <p>Phase 2 sẽ thay bằng wiring WebSocket /game handler sang engine
 * domain. Phase 4 sẽ thêm JPA + Thymeleaf. Phase 5 mới cutover (xóa
 * {@code server/} Javalin implementation).</p>
 */
@SpringBootApplication
public class TankzorsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TankzorsApplication.class, args);
    }
}
