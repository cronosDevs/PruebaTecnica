package com.example.demo.service;

import com.example.demo.dto.InventoryMovementResponse;
import com.example.demo.dto.ProductDTO;
import com.example.demo.entity.InventoryMovement;
import com.example.demo.entity.MovementType;
import com.example.demo.entity.Product;
import com.example.demo.entity.Role;
import com.example.demo.entity.Sale;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.InventoryMovementRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    private static final UserPrincipal ADMIN =
            new UserPrincipal(1L, "admin", "hash", "Administrador", Role.ADMIN, true);

    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryMovementRepository inventoryMovementRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Captor
    private ArgumentCaptor<InventoryMovement> movimientoCaptor;

    @Test
    @DisplayName("crear un producto con stock registra una ENTRADA en el kardex")
    void crearRegistraEntrada() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(1L);
            return product;
        });
        when(userRepository.getReferenceById(1L)).thenReturn(User.builder().id(1L).build());

        productService.create(dto(5), ADMIN);

        verify(inventoryMovementRepository).save(movimientoCaptor.capture());
        assertThat(movimientoCaptor.getValue().getTipo()).isEqualTo(MovementType.ENTRADA);
        assertThat(movimientoCaptor.getValue().getCantidad()).isEqualTo(5);
        assertThat(movimientoCaptor.getValue().getStockResultante()).isEqualTo(5);
    }

    @Test
    @DisplayName("crear un producto sin stock no registra movimiento")
    void crearSinStockNoRegistraMovimiento() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.create(dto(0), ADMIN);

        verify(inventoryMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("cambiar el stock al editar registra un AJUSTE con la diferencia")
    void editarStockRegistraAjuste() {
        Product laptop = producto(10);
        when(productRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(laptop));
        when(productRepository.save(laptop)).thenReturn(laptop);
        when(userRepository.getReferenceById(1L)).thenReturn(User.builder().id(1L).build());

        productService.update(1L, dto(7), ADMIN);

        verify(inventoryMovementRepository).save(movimientoCaptor.capture());
        assertThat(movimientoCaptor.getValue().getTipo()).isEqualTo(MovementType.AJUSTE);
        assertThat(movimientoCaptor.getValue().getCantidad()).isEqualTo(-3);
        assertThat(movimientoCaptor.getValue().getStockResultante()).isEqualTo(7);
    }

    @Test
    @DisplayName("editar sin cambiar el stock no registra movimiento")
    void editarSinCambioDeStock() {
        Product laptop = producto(10);
        when(productRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(laptop));
        when(productRepository.save(laptop)).thenReturn(laptop);

        productService.update(1L, dto(10), ADMIN);

        verify(inventoryMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("eliminar hace borrado lógico")
    void eliminarEsBorradoLogico() {
        Product laptop = producto(10);
        when(productRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(laptop));

        productService.delete(1L, ADMIN);

        assertThat(laptop.getActivo()).isFalse();
        verify(productRepository).save(laptop);
        verify(productRepository, never()).delete(any());
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("un producto eliminado responde 404")
    void productoEliminadoNoExiste() {
        when(productRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Producto no encontrado con id: 1");
    }

    @Test
    @DisplayName("el kardex devuelve los movimientos con su usuario y la venta que los originó")
    void kardexDelProducto() {
        User admin = User.builder().id(1L).username("admin").build();
        User ana = User.builder().id(2L).username("ana").build();
        Sale venta = Sale.builder().id(7L).codigo("VTA-20261112-ABCDEF12").build();
        when(productRepository.existsById(1L)).thenReturn(true);
        when(inventoryMovementRepository.findByProductoIdOrderByFechaDescIdDesc(1L)).thenReturn(List.of(
                InventoryMovement.builder().id(2L).tipo(MovementType.VENTA).cantidad(-2).stockResultante(8)
                        .usuario(ana).venta(venta).build(),
                InventoryMovement.builder().id(1L).tipo(MovementType.ENTRADA).cantidad(10).stockResultante(10)
                        .usuario(admin).build()));

        List<InventoryMovementResponse> kardex = productService.findMovements(1L);

        assertThat(kardex).extracting("tipo", "cantidad", "stockResultante", "usuario", "ventaCodigo")
                .containsExactly(
                        tuple("VENTA", -2, 8, "ana", "VTA-20261112-ABCDEF12"),
                        tuple("ENTRADA", 10, 10, "admin", null));
    }

    @Test
    @DisplayName("el kardex de un producto inexistente responde 404")
    void kardexProductoInexistente() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.findMovements(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Producto no encontrado con id: 99");
    }

    private static ProductDTO dto(int stock) {
        return ProductDTO.builder().nombre("Laptop").precio(new BigDecimal("1000.00")).stock(stock).build();
    }

    private static Product producto(int stock) {
        return Product.builder().id(1L).nombre("Laptop").precio(new BigDecimal("1000.00")).stock(stock).build();
    }
}
