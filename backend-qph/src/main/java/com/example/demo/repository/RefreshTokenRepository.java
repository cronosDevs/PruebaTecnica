package com.example.demo.repository;

import com.example.demo.entity.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @EntityGraph(attributePaths = {"usuario", "usuario.rol"})
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.fechaRevocacion = :ahora WHERE t.id = :id AND t.fechaRevocacion IS NULL")
    int revokeIfActive(@Param("id") Long id, @Param("ahora") OffsetDateTime ahora);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.fechaRevocacion = :ahora WHERE t.usuario.id = :usuarioId AND t.fechaRevocacion IS NULL")
    int revokeAllByUsuario(@Param("usuarioId") Long usuarioId, @Param("ahora") OffsetDateTime ahora);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.fechaExpiracion < :limite")
    int deleteExpired(@Param("limite") OffsetDateTime limite);
}
