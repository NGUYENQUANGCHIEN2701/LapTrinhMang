package com.tankzors.web;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * Đưa các model attributes chuẩn cho mọi Thymeleaf page:
 * {@code pageTitle} và {@code activePage} (dùng bởi layout + navbar).
 */
@Component
public class PageModelFactory {

    public void apply(Model model, String pageTitle, String activePage) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("activePage", activePage);
    }
}
