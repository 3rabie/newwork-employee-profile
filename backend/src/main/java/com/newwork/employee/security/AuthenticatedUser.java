package com.newwork.employee.security;

import com.newwork.employee.entity.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Authenticated principal exposed to controllers/GraphQL layers.
 */
@Getter
public class AuthenticatedUser implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String employeeId;
    private final Role role;
    private final UUID managerId;
    private final List<GrantedAuthority> authorities;

    public AuthenticatedUser(UUID userId,
                             String email,
                             String employeeId,
                             Role role,
                             UUID managerId) {
        this.userId = userId;
        this.email = email;
        this.employeeId = employeeId;
        this.role = role;
        this.managerId = managerId;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
