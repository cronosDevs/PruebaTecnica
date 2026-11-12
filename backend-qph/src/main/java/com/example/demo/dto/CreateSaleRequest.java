package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSaleRequest {

    @NotEmpty(message = "La venta debe tener al menos un producto")
    @Size(max = 50, message = "La venta no puede tener más de 50 productos")
    private List<@Valid SaleItemRequest> items;
}
