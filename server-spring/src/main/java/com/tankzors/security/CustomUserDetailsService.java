package com.tankzors.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Load user từ database cho Spring Security authentication.
 * Map {@link UserEntity#getRole()} thành {@link GrantedAuthority}.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity entity = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Không tìm thấy tài khoản: " + username));

        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(entity.getRole())
        );

        return User.builder()
            .username(entity.getUsername())
            .password(entity.getPassword())
            .disabled(!entity.isEnabled())
            .authorities(authorities)
            .build();
    }
}
