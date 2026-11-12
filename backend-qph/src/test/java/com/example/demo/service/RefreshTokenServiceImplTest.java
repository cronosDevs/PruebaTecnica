package com.example.demo.service;

import com.example.demo.config.JwtProperties;
import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;

    private RefreshTokenServiceImpl service;

    private final User ana = User.builder().id(2L).username("ana").activo(true).build();

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties("c2VjcmV0", 900_000, 604_800_000, true);
        service = new RefreshTokenServiceImpl(refreshTokenRepository, userRepository, properties);
    }

    @Test
    @DisplayName("al crear guarda solo el hash SHA-256 del token, nunca el valor")
    void guardaSoloElHash() {
        when(userRepository.getReferenceById(2L)).thenReturn(ana);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        String token = service.create(2L);

        verify(refreshTokenRepository).save(captor.capture());
        assertThat(token).hasSize(43);
        assertThat(captor.getValue().getTokenHash())
                .isEqualTo(RefreshTokenServiceImpl.hash(token))
                .hasSize(64)
                .isNotEqualTo(token);
        assertThat(captor.getValue().getFechaExpiracion()).isAfter(OffsetDateTime.now().plusDays(6));
    }

    @Test
    @DisplayName("rotar revoca el token usado y entrega uno nuevo")
    void rotacionExitosa() {
        RefreshToken actual = token(10L, null, OffsetDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHash(RefreshTokenServiceImpl.hash("valido"))).thenReturn(Optional.of(actual));
        when(refreshTokenRepository.revokeIfActive(eq(10L), any())).thenReturn(1);

        RefreshRotation rotacion = service.rotate("valido");

        assertThat(rotacion.usuario()).isSameAs(ana);
        assertThat(rotacion.token()).isNotEqualTo("valido");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(refreshTokenRepository, never()).revokeAllByUsuario(anyLong(), any());
    }

    @Test
    @DisplayName("un token que no existe responde SESION_INVALIDA")
    void tokenDesconocido() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("inventado"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SESION_INVALIDA);
    }

    @Test
    @DisplayName("reusar un token ya revocado cierra todas las sesiones del usuario")
    void reusoDetectado() {
        RefreshToken revocado = token(10L, OffsetDateTime.now().minusMinutes(5), OffsetDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revocado));

        assertThatThrownBy(() -> service.rotate("robado"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SESION_INVALIDA);

        verify(refreshTokenRepository).revokeAllByUsuario(eq(2L), any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("si otra petición rotó el mismo token al mismo tiempo se trata como reuso")
    void rotacionConcurrente() {
        RefreshToken actual = token(10L, null, OffsetDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(actual));
        when(refreshTokenRepository.revokeIfActive(eq(10L), any())).thenReturn(0);

        assertThatThrownBy(() -> service.rotate("duplicado")).isInstanceOf(BusinessException.class);

        verify(refreshTokenRepository).revokeAllByUsuario(eq(2L), any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("un token vencido responde SESION_INVALIDA sin emitir uno nuevo")
    void tokenVencido() {
        RefreshToken vencido = token(10L, null, OffsetDateTime.now().minusMinutes(1));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(vencido));

        assertThatThrownBy(() -> service.rotate("vencido")).isInstanceOf(BusinessException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("un usuario desactivado no puede renovar la sesión")
    void usuarioDesactivado() {
        ana.setActivo(false);
        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(token(10L, null, OffsetDateTime.now().plusDays(1))));

        assertThatThrownBy(() -> service.rotate("valido"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USUARIO_INACTIVO);
    }

    private RefreshToken token(Long id, OffsetDateTime revocacion, OffsetDateTime expiracion) {
        return RefreshToken.builder()
                .id(id).usuario(ana).tokenHash("hash").fechaRevocacion(revocacion).fechaExpiracion(expiracion)
                .build();
    }
}
