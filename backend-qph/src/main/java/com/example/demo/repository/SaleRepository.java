package com.example.demo.repository;

import com.example.demo.entity.Sale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findByUsuarioIdAndClaveIdempotencia(Long usuarioId, String claveIdempotencia);

    @EntityGraph(attributePaths = {"usuario", "items", "items.producto"})
    Optional<Sale> findDetailById(Long id);

    @EntityGraph(attributePaths = {"usuario", "items", "items.producto"})
    List<Sale> findAllByOrderByFechaCreacionDesc();

    @EntityGraph(attributePaths = {"usuario", "items", "items.producto"})
    List<Sale> findAllByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);
}
