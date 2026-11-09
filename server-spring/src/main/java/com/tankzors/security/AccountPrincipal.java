package com.tankzors.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.List;

/** Database identity prevents a deleted username's old session from being reused. */
public final class AccountPrincipal extends User {
    private final Long accountId;
    public AccountPrincipal(UserEntity account) {
        super(account.getUsername(), account.getPassword(), account.isEnabled(), true, true, true,
                List.of(new SimpleGrantedAuthority(account.getRole())));
        accountId = account.getId();
    }
    public Long getAccountId() { return accountId; }
}
