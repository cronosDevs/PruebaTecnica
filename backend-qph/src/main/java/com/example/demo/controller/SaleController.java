package com.example.demo.controller;

import com.example.demo.dto.CreateSaleRequest;
import com.example.demo.dto.SaleResponse;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Ventas", description = "Registro e historial de ventas. ADMIN y VENDEDOR")
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @Operation(summary = "Registrar una venta",
            description = """
                    Descuenta el stock de forma atómica y registra cada movimiento en el kardex. \
                    Si un producto no tiene stock suficiente se revierte la venta completa. \
                    Con el header Idempotency-Key, un reintento con la misma clave devuelve la venta original \
                    sin descontar stock de nuevo.""")
    @ApiResponse(responseCode = "201", description = "Venta registrada, o venta original si la Idempotency-Key ya se usó")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Un producto no existe o fue eliminado")
    @ApiResponse(responseCode = "409", description = "Stock insuficiente, o venta en proceso con la misma Idempotency-Key")
    @PostMapping
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody CreateSaleRequest request,
                                               @Parameter(description = "Clave única por intento de venta, máximo 64 caracteres",
                                                       example = "3f2b9c1e-7d4a-4e8b-9a61-0c5d2e8f1a77")
                                               @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                               @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal actor) {
        SaleResponse sale = saleService.create(request, idempotencyKey, actor);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(sale.getId())
                .toUri();
        return ResponseEntity.created(location).body(sale);
    }

    @Operation(summary = "Historial de ventas",
            description = "ADMIN ve todas las ventas. VENDEDOR ve solo las propias. Ordenado de la más reciente a la más antigua.")
    @GetMapping
    public ResponseEntity<List<SaleResponse>> getAll(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal actor) {
        return ResponseEntity.ok(saleService.findAll(actor));
    }

    @Operation(summary = "Detalle de una venta")
    @ApiResponse(responseCode = "200", description = "Venta encontrada")
    @ApiResponse(responseCode = "404", description = "La venta no existe o pertenece a otro vendedor")
    @GetMapping("/{id}")
    public ResponseEntity<SaleResponse> getById(@PathVariable Long id,
                                                @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal actor) {
        return ResponseEntity.ok(saleService.findById(id, actor));
    }
}
