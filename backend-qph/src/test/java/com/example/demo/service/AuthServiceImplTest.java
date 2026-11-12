package com.example.demo.service;

import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.AuthSession;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.security.JwtService;
import com.example.demo.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("con credenciales válidas devuelve el access token, los datos del usuario y un refresh token")
    void loginExitoso() {
        UserPrincipal ana = new UserPrincipal(2L, "ana", "hash", "Ana Torres", Role.VENDEDOR, true);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(ana, null, ana.getAuthorities()));
        when(jwtService.generateToken(ana)).thenReturn("jwt-firmado");
        when(refreshTokenService.create(2L)).thenReturn("refresh-aleatorio");

        AuthSession sesion = authService.login(solicitud("ana", "Ventas2026"));

        assertThat(sesion.body().getToken()).isEqualTo("jwt-firmado");
        assertThat(sesion.body().getUsername()).isEqualTo("ana");
        assertThat(sesion.body().getRol()).isEqualTo("VENDEDOR");
        assertThat(sesion.refreshToken()).isEqualTo("refresh-aleatorio");
    }

    @Test
    @DisplayName("con credenciales inválidas propaga el error y no genera tokens")
    void loginFallido() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(solicitud("ana", "mala")))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
        verify(refreshTokenService, never()).create(any());
    }

    @Test
    @DisplayName("refresh entrega un access token nuevo para el usuario del refresh token rotado")
    void refreshEntregaNuevoAccessToken() {
        User ana = User.builder().id(2L).username("ana").passwordHash("hash").nombreCompleto("Ana Torres")
                .rol(Role.builder().nombre(Role.VENDEDOR).build()).activo(true).build();
        when(refreshTokenService.rotate("refresh-viejo")).thenReturn(new RefreshRotation(ana, "refresh-nuevo"));
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("jwt-nuevo");

        AuthSession sesion = authService.refresh("refresh-viejo");

        assertThat(sesion.body().getToken()).isEqualTo("jwt-nuevo");
        assertThat(sesion.body().getUsername()).isEqualTo("ana");
        assertThat(sesion.refreshToken()).isEqualTo("refresh-nuevo");
    }

    private static AuthRequest solicitud(String username, String password) {
        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}
