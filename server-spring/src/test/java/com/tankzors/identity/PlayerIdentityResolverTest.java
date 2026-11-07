package com.tankzors.identity;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerIdentityResolverTest {

    private final PlayerIdentityResolver resolver = new PlayerIdentityResolver();

    @Test
    void authenticatedUsernameIsStableAccountKeyAndBeatsBrowserSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Player-Session", "temporary-browser-id");
        request.setUserPrincipal(() -> "tank-commander");

        assertEquals("user:tank-commander", resolver.resolve(request));
    }

    @Test
    void clientHeaderCannotChooseIdentity() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Player-Session", "user:victim");

        org.junit.jupiter.api.Assertions.assertNotEquals("user:victim", resolver.resolve(request));
    }
}
