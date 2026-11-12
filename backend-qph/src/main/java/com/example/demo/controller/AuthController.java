package com.example.demo.controller;

import com.example.demo.config.JwtProperties;
import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.AuthSession;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "Autenticación", description = "Inicio de sesión, renovación y cierre de sesión")
@SecurityRequirements
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @Operation(summary = "Iniciar sesión",
            description = "Valida usuario y contraseña. Devuelve un access token de corta duración en el cuerpo "
                    + "y un refresh token en una cookie HttpOnly.")
    @ApiResponse(responseCode = "200", description = "Credenciales válidas")
    @ApiResponse(responseCode = "400", description = "Faltan el usuario o la contraseña")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas o usuario desactivado")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return withRefreshCookie(authService.login(request));
    }

    @Operation(summary = "Renovar el access token",
            description = "Usa el refresh token de la cookie, lo revoca y entrega uno nuevo junto con un access token nuevo.")
    @ApiResponse(responseCode = "200", description = "Sesión renovada")
    @ApiResponse(responseCode = "401", description = "Refresh token ausente, vencido, revocado o reutilizado")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Parameter(hidden = true) @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.SESION_INVALIDA);
        }
        return withRefreshCookie(authService.refresh(refreshToken));
    }

    @Operation(summary = "Cerrar sesión", description = "Revoca el refresh token en el servidor y elimina la cookie.")
    @ApiResponse(responseCode = "204", description = "Sesión cerrada")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(hidden = true) @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthSession session) {
        ResponseCookie cookie = refreshCookie(session.refreshToken(),
                Duration.ofMillis(jwtProperties.refreshExpiration()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(session.body());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(jwtProperties.refreshCookieSecure())
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
