package com.tankzors.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Bắt exception từ page controller (Thymeleaf) và render error page
 * thân thiện. Không in stack trace ra UI.
 *
 * <p>Chỉ áp dụng cho {@link PageController} để không phá error semantics
 * của các {@code @RestController} (Health/Leaderboard/Progress).</p>
 */
@ControllerAdvice(assignableTypes = PageController.class)
public class WebExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public String handlePageException(Exception ex, Model model) {
        logger.error("[Web] unhandled page error: {}", ex.getMessage(), ex);
        model.addAttribute("pageTitle", "Lỗi hệ thống");
        model.addAttribute("activePage", "error");
        model.addAttribute("errorMessage",
            "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.");
        return "error/500";
    }
}
