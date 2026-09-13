package com.tankzors.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Cấu hình Gson bean dùng chung cho WebSocket handler Phase 2.
 *
 * <p>Bean phải cùng định dạng với {@code MessageSender} cũ (không
 * prettyPrint, không serialize null). Tránh format khác protocol-v1.</p>
 */
@Configuration
public class GsonConfig {

    @Bean
    public Gson gson() {
        return new GsonBuilder().create();
    }
}
