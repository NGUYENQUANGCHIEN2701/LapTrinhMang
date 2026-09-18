package com.tankzors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test-only SecurityConfig: permit all requests (không cần login).
 *
 * <p>Được kích hoạt khiprofile "test" active. Production SecurityConfig
 * có @Profile("!test") nên sẽ không cạnh tranh.</p>
 *
 * <p>Cho phép các test hiện tại (PageController, Leaderboard, etc.) chạy
 * không bị redirect 302 → /login như trong production config.</p>
 */
@Configuration
@Profile("test")
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
