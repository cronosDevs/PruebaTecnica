package com.example.demo.service;

import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.AuthSession;
import com.example.demo.security.JwtService;
import com.example.demo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public AuthSession login(AuthRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException ex) {
            log.warn("Inicio de sesión fallido: usuario={}, motivo={}",
                    sanitize(request.getUsername()), ex.getClass().getSimpleName());
            throw ex;
        }

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        log.info("Inicio de sesión exitoso: usuario={}, rol={}", user.getUsername(), user.getRol());
        return new AuthSession(toResponse(user), refreshTokenService.create(user.getId()));
    }

    @Override
    public AuthSession refresh(String refreshToken) {
        RefreshRotation rotation = refreshTokenService.rotate(refreshToken);
        UserPrincipal user = UserPrincipal.from(rotation.usuario());
        return new AuthSession(toResponse(user), rotation.token());
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthResponse toResponse(UserPrincipal user) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .username(user.getUsername())
                .nombreCompleto(user.getNombreCompleto())
                .rol(user.getRol())
                .build();
    }

    private static String sanitize(String value) {
        String limpio = value.replaceAll("[\\r\\n\\t]", "_");
        return limpio.length() > 50 ? limpio.substring(0, 50) + "..." : limpio;
    }
}
