package com.example.demo.service;

import com.example.demo.config.JwtProperties;
import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public String create(Long usuarioId) {
        return issue(userRepository.getReferenceById(usuarioId));
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public RefreshRotation rotate(String token) {
        RefreshToken actual = refreshTokenRepository.findByTokenHash(hash(token))
                .orElseThrow(RefreshTokenServiceImpl::invalidSession);
        User usuario = actual.getUsuario();
        OffsetDateTime ahora = OffsetDateTime.now();

        if (actual.getFechaRevocacion() != null) {
            revokeAllAfterReuse(usuario);
        }
        if (actual.getFechaExpiracion().isBefore(ahora)) {
            throw invalidSession();
        }
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BusinessException(ErrorCode.USUARIO_INACTIVO);
        }
        if (refreshTokenRepository.revokeIfActive(actual.getId(), ahora) == 0) {
            revokeAllAfterReuse(usuario);
        }

        log.debug("Refresh token rotado: usuario={}", usuario.getUsername());
        return new RefreshRotation(usuario, issue(usuario));
    }

    @Override
    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByTokenHash(hash(token)).ifPresent(actual -> {
            if (refreshTokenRepository.revokeIfActive(actual.getId(), OffsetDateTime.now()) > 0) {
                log.info("Sesión cerrada: usuario={}", actual.getUsuario().getUsername());
            }
        });
    }

    @Override
    @Transactional
    public void revokeAll(Long usuarioId) {
        int revocados = refreshTokenRepository.revokeAllByUsuario(usuarioId, OffsetDateTime.now());
        if (revocados > 0) {
            log.info("Sesiones revocadas: usuarioId={}, cantidad={}", usuarioId, revocados);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpired() {
        int eliminados = refreshTokenRepository.deleteExpired(OffsetDateTime.now());
        if (eliminados > 0) {
            log.info("Refresh tokens vencidos eliminados: {}", eliminados);
        }
    }

    private String issue(User usuario) {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        refreshTokenRepository.save(RefreshToken.builder()
                .usuario(usuario)
                .tokenHash(hash(token))
                .fechaExpiracion(OffsetDateTime.now().plusNanos(jwtProperties.refreshExpiration() * 1_000_000))
                .build());
        return token;
    }

    private void revokeAllAfterReuse(User usuario) {
        int revocados = refreshTokenRepository.revokeAllByUsuario(usuario.getId(), OffsetDateTime.now());
        log.warn("Reuso de refresh token detectado: usuario={}, sesiones revocadas={}", usuario.getUsername(), revocados);
        throw invalidSession();
    }

    private static BusinessException invalidSession() {
        return new BusinessException(ErrorCode.SESION_INVALIDA);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
