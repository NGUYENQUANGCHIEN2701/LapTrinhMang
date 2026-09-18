package com.tankzors.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

/**
 * Seed dữ liệu mặc định khi khởi động:
 *
 * <ul>
 *   <li>Tạo tài khoản admin mặc định {@code admin / admin123} nếu chưa tồn tại.</li>
 * </ul>
 *
 * <p>Log cảnh báo thông tin đăng nhập nếu đây là tài khoản được tạo mới.</p>
 */
@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedAdminAccount(UserRepository userRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {
            String adminUsername = "admin";
            if (userRepository.existsByUsername(adminUsername)) {
                logger.info("[DataInitializer] Admin account '{}' already exists, skip seed.", adminUsername);
                return;
            }

            UserEntity admin = new UserEntity();
            admin.setUsername(adminUsername);
            admin.setEmail("admin@tankzors.local");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setDisplayName("Administrator");
            admin.setRole("ROLE_ADMIN");
            admin.setEnabled(true);
            Instant now = Instant.now();
            admin.setCreatedAt(now);
            admin.setUpdatedAt(now);

            userRepository.save(admin);

            logger.warn("========================================================");
            logger.warn("  [DataInitializer] Default admin account created!");
            logger.warn("  Username: admin");
            logger.warn("  Password: admin123");
            logger.warn("  >>> CHANGE THE PASSWORD IMMEDIATELY! <<<");
            logger.warn("========================================================");
        };
    }
}
