package com.example.demo.security;

import com.example.demo.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@AllArgsConstructor
@ToString(exclude = "password")
public class UserPrincipal implements UserDetails {

    private static final String ROLE_PREFIX = "ROLE_";

    private final Long id;
    private final String username;
    private final String password;
    private final String nombreCompleto;
    private final String rol;
    private final boolean activo;

    public static UserPrincipal from(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getNombreCompleto(),
                user.getRol().getNombre(),
                Boolean.TRUE.equals(user.getActivo()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(ROLE_PREFIX + rol));
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
