package com.tankzors.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Dùng SecurityConfig thật (không profile "test"); cùng properties với LoginFailureMessageTest để chung context. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:tankzors_login;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class ApiAuthenticationTest {

    @Autowired private MockMvc mvc;

    @Test
    void expiredSessionOnApiGets401NotLoginRedirect() throws Exception {
        mvc.perform(post("/api/game/results").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/game/bootstrap")).andExpect(status().isUnauthorized());
    }

    @Test
    void pagesStillRedirectToLogin() throws Exception {
        // Trình duyệt mở trang luôn gửi Accept: text/html.
        mvc.perform(get("/campaigns").accept(org.springframework.http.MediaType.TEXT_HTML)).andExpect(redirectedUrlPattern("**/login"));
    }
}
