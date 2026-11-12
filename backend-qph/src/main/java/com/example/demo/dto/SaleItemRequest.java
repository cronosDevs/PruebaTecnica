package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleItemRequest {

    @NotNull(message = "El producto es requerido")
    private Long productoId;

    @NotNull(message = "La cantidad es requerida")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    @Max(value = 10000, message = "La cantidad no puede superar 10000 unidades")
    private Integer cantidad;
}
