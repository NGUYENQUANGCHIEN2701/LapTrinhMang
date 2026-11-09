package com.tankzors.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Recheck persisted status for authenticated sessions, including existing cookies. */
public final class AccountStatusFilter extends OncePerRequestFilter {
    private final UserRepository users;
    public AccountStatusFilter(UserRepository users) { this.users = users; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/game/libgdx/") || path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.equals("/favicon.ico") || path.equals("/api/health");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            UserEntity user = users.findById(principal.getAccountId()).orElse(null);
            if (user == null || !user.isEnabled() || !user.getUsername().equals(principal.getUsername())) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                if (request.getRequestURI().startsWith("/api/")) response.sendError(401);
                else response.sendRedirect(request.getContextPath() + "/login?disabled=true");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
