package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovementResponse {
    private Long id;
    private String tipo;
    private Integer cantidad;
    private Integer stockResultante;
    private OffsetDateTime fecha;
    private String usuario;
    private String ventaCodigo;
}
