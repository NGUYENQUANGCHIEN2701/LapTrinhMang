package com.tankzors.web;

import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;

/**
 * Lưu trữ {@link GameSettings} trong HttpSession.
 *
 * <p>Theo plan Phase 4.5: chưa tạo bảng database settings;
 * session đủ dùng cho giai đoạn đầu.</p>
 */
@Service
public class SettingsService {

    private static final String SESSION_KEY = "tankzors.gameSettings";

    /** Load settings của session; trả default nếu chưa từng lưu. */
    public GameSettings load(HttpSession session) {
        Object value = session.getAttribute(SESSION_KEY);
        return value instanceof GameSettings settings ? settings : GameSettings.defaults();
    }

    /** Lưu settings (đã clamp) vào session. */
    public void save(HttpSession session, GameSettings settings) {
        session.setAttribute(SESSION_KEY, settings);
    }
}
