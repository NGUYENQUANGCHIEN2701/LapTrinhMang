package com.tankzors.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegisterTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;

    private org.springframework.test.web.servlet.ResultActions register(String username) throws Exception {
        return mvc.perform(post("/register").param("username", username)
                .param("password", "secret123").param("confirmPassword", "secret123"));
    }

    @Test
    void usernameWithSurroundingSpacesIsTrimmedAndCheckedForDuplicates() throws Exception {
        register("  trim-user  ").andExpect(redirectedUrl("/login"));
        assertTrue(users.existsByUsername("trim-user"));

        // Trước đây qua được kiểm tra trùng rồi vỡ ràng buộc UNIQUE (lỗi 500).
        register(" trim-user").andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("flashError", "Tên đăng nhập đã tồn tại."));
    }

    @Test
    void spacesDoNotCountTowardMinimumLength() throws Exception {
        register("  ab  ").andExpect(redirectedUrl("/register"));
    }
    @Test void oversizedDisplayNameAndEmailReturnFormError() throws Exception {
        mvc.perform(post("/register").param("username", "long-profile")
                .param("password", "secret123").param("confirmPassword", "secret123")
                .param("displayName", "x".repeat(81)))
                .andExpect(redirectedUrl("/register")).andExpect(flash().attributeExists("flashError"));
        mvc.perform(post("/register").param("username", "long-email")
                .param("password", "secret123").param("confirmPassword", "secret123")
                .param("email", "x".repeat(101)))
                .andExpect(redirectedUrl("/register")).andExpect(flash().attributeExists("flashError"));
    }
}
