package com.example.demo;

import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.AuthSession;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthIntegrationTest extends IntegrationTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
    }

    @Test
    @DisplayName("refresh entrega un token nuevo y el anterior queda revocado en la base")
    void rotacion() {
        AuthSession login = authService.login(admin());

        AuthSession renovada = authService.refresh(login.refreshToken());

        assertThat(renovada.refreshToken()).isNotEqualTo(login.refreshToken());
        assertThat(renovada.body().getToken()).isNotBlank();
        assertThat(contar("SELECT count(*) FROM refresh_tokens WHERE fecha_revocacion IS NULL")).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM refresh_tokens WHERE fecha_revocacion IS NOT NULL")).isEqualTo(1);
    }

    @Test
    @DisplayName("reusar un refresh token revocado cierra todas las sesiones y el cambio persiste aunque haya error")
    void reusoCierraTodasLasSesiones() {
        AuthSession login = authService.login(admin());
        AuthSession otroDispositivo = authService.login(admin());
        AuthSession renovada = authService.refresh(login.refreshToken());

        assertThatThrownBy(() -> authService.refresh(login.refreshToken()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SESION_INVALIDA);

        assertThat(contar("SELECT count(*) FROM refresh_tokens WHERE fecha_revocacion IS NULL")).isZero();
        assertThatThrownBy(() -> authService.refresh(renovada.refreshToken())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> authService.refresh(otroDispositivo.refreshToken())).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("después del logout el refresh token ya no sirve")
    void logoutRevocaElToken() {
        AuthSession login = authService.login(admin());

        authService.logout(login.refreshToken());

        assertThatThrownBy(() -> authService.refresh(login.refreshToken()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SESION_INVALIDA);
    }

    @Test
    @DisplayName("la base guarda el hash del refresh token, no el valor")
    void seGuardaElHash() {
        AuthSession login = authService.login(admin());

        assertThat(contar("SELECT count(*) FROM refresh_tokens WHERE token_hash = ?", login.refreshToken())).isZero();
        assertThat(contar("SELECT count(*) FROM refresh_tokens WHERE length(token_hash) = 64")).isEqualTo(1);
    }

    private static AuthRequest admin() {
        AuthRequest request = new AuthRequest();
        request.setUsername("admin");
        request.setPassword("admin123");
        return request;
    }

    private int contar(String sql, Object... parametros) {
        Integer total = jdbcTemplate.queryForObject(sql, Integer.class, parametros);
        return total == null ? 0 : total;
    }
}
