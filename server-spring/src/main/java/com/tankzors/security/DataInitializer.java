package com.tankzors.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

/**
 * Seed dữ liệu mặc định khi khởi động:
 *
 * <ul>
 *   <li>Tạo tài khoản {@code admin} nếu chưa tồn tại, mật khẩu lấy từ biến môi trường
 *       {@code ADMIN_PASSWORD}. Biến rỗng → không tạo (không có mật khẩu mặc định trong code).</li>
 * </ul>
 */
@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedAdminAccount(UserRepository userRepository,
                                       PasswordEncoder passwordEncoder,
                                       @Value("${ADMIN_PASSWORD:}") String adminPassword) {
        return args -> {
            String adminUsername = "admin";
            if (adminPassword == null || adminPassword.isBlank()) {
                logger.info("[DataInitializer] ADMIN_PASSWORD trống, bỏ qua tạo tài khoản admin.");
                return;
            }
            if (userRepository.existsByUsername(adminUsername)) {
                logger.info("[DataInitializer] Admin account '{}' already exists, skip seed.", adminUsername);
                return;
            }

            UserEntity admin = new UserEntity();
            admin.setUsername(adminUsername);
            admin.setEmail("admin@tankzors.local");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setDisplayName("Administrator");
            admin.setRole("ROLE_ADMIN");
            admin.setEnabled(true);
            Instant now = Instant.now();
            admin.setCreatedAt(now);
            admin.setUpdatedAt(now);

            userRepository.save(admin);

            logger.warn("[DataInitializer] Đã tạo tài khoản admin từ ADMIN_PASSWORD.");
        };
    }
}
