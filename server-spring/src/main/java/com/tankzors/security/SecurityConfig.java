package com.tankzors.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security configuration cho Tankzors Pro.
 *
 * <p>Phân quyền theo 2 role:</p>
 * <ul>
 *   <li><b>ROLE_USER</b> — Chơi game, leaderboard, settings, campaigns</li>
 *   <li><b>ROLE_ADMIN</b> — Toàn quyền + truy cập /admin/** (dashboard quản lý)</li>
 * </ul>
 *
 * <p>Auth scheme: Session-based (cookie JSESSIONID), phù hợp SSR Thymeleaf.</p>
 */
@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Static resources — public
                .requestMatchers("/css/**", "/js/**", "/images/**", "/play/**", "/game/**",
                                 "/favicon.ico", "/webjars/**").permitAll()
                // Public pages
                .requestMatchers("/", "/menu", "/register", "/error/**").permitAll()
                // Admin-only
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/menu", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf
                // WS handshake endpoints cần bỏ CSRF (WebSocket nguyên gốc không gửi CSRF token)
                // Client LibGDX HTML5 gửi JSON trực tiếp từ iframe cùng origin.
                // Nó không có cơ chế đọc/đính kèm CSRF token của form Thymeleaf;
                // nếu chặn POST /api/game/results, chiến thắng không được ghi nhận
                // và màn tiếp theo luôn bị bootstrap từ chối (403).
                .ignoringRequestMatchers("/game", "/coop", "/api/game/**")
            );

        return http.build();
    }
}
