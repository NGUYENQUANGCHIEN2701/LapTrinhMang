package com.tankzors.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Không dùng profile "test": profile đó thay SecurityConfig thật bằng cấu hình permit-all. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:tankzors_login;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class LoginFailureMessageTest {

    private static final String DISABLED_MESSAGE = "Tài khoản của bạn đã bị vô hiệu hóa.";
    private static final String BAD_CREDENTIALS_MESSAGE = "Sai tên đăng nhập hoặc mật khẩu.";

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder encoder;

    private void saveUser(String username, boolean enabled) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(encoder.encode("secret123"));
        user.setDisplayName(username);
        user.setRole("ROLE_USER");
        user.setEnabled(enabled);
        user.setCreatedAt(java.time.Instant.now());
        user.setUpdatedAt(java.time.Instant.now());
        users.save(user);
    }

    private org.springframework.test.web.servlet.ResultActions login(String username, String password)
            throws Exception {
        return mvc.perform(post("/login").with(csrf()).param("username", username).param("password", password));
    }

    @Test
    void disabledUserIsSentToDisabledMessage() throws Exception {
        saveUser("disabled-user", false);
        login("disabled-user", "secret123").andExpect(redirectedUrl("/login?disabled=true"));
    }

    @Test
    void wrongPasswordKeepsBadCredentialsMessage() throws Exception {
        saveUser("active-user", true);
        login("active-user", "wrong-password").andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void enabledUserStillLogsIn() throws Exception {
        saveUser("happy-user", true);
        login("happy-user", "secret123").andExpect(redirectedUrl("/menu"));
    }

    @Test
    void disabledPageIsPublicAndShowsOnlyDisabledMessage() throws Exception {
        mvc.perform(get("/login?disabled=true"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(DISABLED_MESSAGE)))
            .andExpect(content().string(not(containsString(BAD_CREDENTIALS_MESSAGE))));
    }

    @Test
    void errorPageShowsOnlyBadCredentialsMessage() throws Exception {
        mvc.perform(get("/login?error=true"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(BAD_CREDENTIALS_MESSAGE)))
            .andExpect(content().string(not(containsString(DISABLED_MESSAGE))));
    }
}
