package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleResponse {
    private Long id;
    private String codigo;
    private BigDecimal total;
    private String vendedor;
    private String vendedorNombre;
    private OffsetDateTime fechaCreacion;
    private List<SaleItemResponse> items;
}
