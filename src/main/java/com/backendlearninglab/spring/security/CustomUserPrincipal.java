package com.backendlearninglab.spring.security;

import com.backendlearninglab.spring.entity.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapter wrapping UserEntity to implement Spring Security's UserDetails.
 * <p>
 * TEACHING FOCUS: Roles vs. Authorities:
 * <ul>
 *   <li>In Spring Security, a <b>Role</b> is simply an authority prefixed with "ROLE_".</li>
 *   <li>Calling {@code hasRole('ADMIN')} automatically checks for an authority named {@code "ROLE_ADMIN"}.</li>
 *   <li>Calling {@code hasAuthority('product:write')} checks for the exact raw string.</li>
 * </ul>
 */
public class CustomUserPrincipal implements UserDetails {

    private final UserEntity user;

    public CustomUserPrincipal(UserEntity user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Enforce the standard Spring Security "ROLE_" prefix
        String roleName = "ROLE_" + user.getRole().name();
        return List.of(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    public Long getId() {
        return user.getId();
    }

    public UserEntity getUserEntity() {
        return user;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"SUSPENDED".equalsIgnoreCase(user.getStatus());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equalsIgnoreCase(user.getStatus());
    }
}
