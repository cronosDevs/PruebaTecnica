package com.example.demo.repository;

import com.example.demo.entity.InventoryMovement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    @EntityGraph(attributePaths = {"usuario", "venta"})
    List<InventoryMovement> findByProductoIdOrderByFechaDescIdDesc(Long productoId);
}
