package com.tankzors.application;

import com.tankzors.progress.PlayerTotals;
import com.tankzors.progress.PlayerTotalsRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** One database row serializes all account mutations until transaction commit. */
@Component
public class PlayerStateLock {
    private final JdbcTemplate jdbc;
    private final PlayerTotalsRepository totals;
    private final com.tankzors.security.UserRepository users;
    public PlayerStateLock(JdbcTemplate jdbc, PlayerTotalsRepository totals, com.tankzors.security.UserRepository users) {
        this.jdbc = jdbc; this.totals = totals; this.users = users;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public PlayerTotals acquire(String playerId) {
        if (playerId == null || playerId.isBlank() || playerId.length() > 80) throw new IllegalArgumentException("playerId không hợp lệ");
        // PostgreSQL and H2 PostgreSQL mode both support this form of ON CONFLICT.
        jdbc.update("INSERT INTO player_totals(player_id, total_stars, total_coins, revision) VALUES (?, 0, 0, 0) ON CONFLICT DO NOTHING", playerId);
        PlayerTotals wallet = totals.findForUpdate(playerId).orElseThrow();
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.tankzors.security.AccountPrincipal principal
                && users.findById(principal.getAccountId()).filter(com.tankzors.security.UserEntity::isEnabled).isEmpty()) {
            // A request may have passed the HTTP filter before waiting behind admin deletion.
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        return wallet;
    }
}
