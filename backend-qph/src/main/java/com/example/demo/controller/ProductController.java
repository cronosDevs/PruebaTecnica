package com.example.demo.controller;

import com.example.demo.dto.InventoryMovementResponse;
import com.example.demo.dto.ProductDTO;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Productos", description = "Catálogo de productos. Lectura: ADMIN y VENDEDOR. Escritura: solo ADMIN")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Listar productos activos")
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    @Operation(summary = "Obtener un producto activo")
    @ApiResponse(responseCode = "200", description = "Producto encontrado")
    @ApiResponse(responseCode = "404", description = "El producto no existe o fue eliminado")
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @Operation(summary = "Kardex de un producto",
            description = "Movimientos de inventario del producto (entradas, ventas y ajustes), del más reciente al más antiguo. Solo ADMIN.")
    @ApiResponse(responseCode = "200", description = "Movimientos del producto")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    @GetMapping("/{id}/movements")
    public ResponseEntity<List<InventoryMovementResponse>> getMovements(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findMovements(id));
    }

    @Operation(summary = "Crear un producto")
    @ApiResponse(responseCode = "201", description = "Producto creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "El usuario no es ADMIN")
    @PostMapping
    public ResponseEntity<ProductDTO> create(@Valid @RequestBody ProductDTO dto,
                                             @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal actor) {
        ProductDTO created = productService.create(dto, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Actualizar un producto")
    @ApiResponse(responseCode = "200", description = "Producto actualizado")
    @ApiResponse(responseCode = "404", description = "El producto no existe o fue eliminado")
    @ApiResponse(responseCode = "409", description = "Otro usuario modificó el producto al mismo tiempo")
    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> update(@PathVariable Long id,
                                              @Valid @RequestBody ProductDTO dto,
                                              @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal actor) {
        return ResponseEntity.ok(productService.update(id, dto, actor));
    }

    @Operation(summary = "Eliminar un producto",
            description = "Borrado lógico: el producto deja de estar disponible pero se conserva para el historial de ventas.")
    @ApiResponse(responseCode = "204", description = "Producto eliminado")
    @ApiResponse(responseCode = "404", description = "El producto no existe o ya fue eliminado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal actor) {
        productService.delete(id, actor);
        return ResponseEntity.noContent().build();
    }
}
