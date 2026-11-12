package com.example.demo.repository;

import com.example.demo.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByActivoTrueOrderByIdAsc();

    Optional<Product> findByIdAndActivoTrue(Long id);

    @Modifying
    @Query("""
            UPDATE Product p
               SET p.stock = p.stock - :cantidad,
                   p.version = p.version + 1,
                   p.fechaActualizacion = :ahora
             WHERE p.id = :id
               AND p.activo = true
               AND p.stock >= :cantidad
            """)
    int decrementStock(@Param("id") Long id,
                       @Param("cantidad") int cantidad,
                       @Param("ahora") OffsetDateTime ahora);
}
