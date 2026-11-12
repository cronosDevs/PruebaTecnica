package com.example.demo.service;

import com.example.demo.dto.CreateSaleRequest;
import com.example.demo.dto.SaleItemRequest;
import com.example.demo.dto.SaleResponse;
import com.example.demo.entity.InventoryMovement;
import com.example.demo.entity.MovementType;
import com.example.demo.entity.Product;
import com.example.demo.entity.Role;
import com.example.demo.entity.Sale;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.InventoryMovementRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleServiceImplTest {

    private static final UserPrincipal ADMIN =
            new UserPrincipal(1L, "admin", "hash", "Administrador", Role.ADMIN, true);
    private static final UserPrincipal VENDEDORA =
            new UserPrincipal(2L, "ana", "hash", "Ana Torres", Role.VENDEDOR, true);

    @Mock
    private SaleRepository saleRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @InjectMocks
    private SaleServiceImpl saleService;

    @Captor
    private ArgumentCaptor<List<InventoryMovement>> movimientosCaptor;

    private final User ana = User.builder().id(2L).username("ana").nombreCompleto("Ana Torres").build();
    private final User luis = User.builder().id(3L).username("luis").nombreCompleto("Luis Mora").build();

    @Nested
    @DisplayName("Registrar una venta")
    class Create {

        @Test
        @DisplayName("descuenta el stock, congela el precio, calcula el total y registra el kardex")
        void registraVentaCompleta() {
            prepararVenta();
            when(productRepository.decrementStock(eq(1L), eq(2), any())).thenReturn(1);
            when(productRepository.findById(1L)).thenReturn(Optional.of(producto(1L, "Laptop", "1000.00", 3)));

            SaleResponse venta = saleService.create(solicitud(linea(1L, 2)), null, VENDEDORA);

            assertThat(venta.getCodigo()).matches("VTA-\\d{8}-[0-9A-F]{8}");
            assertThat(venta.getTotal()).isEqualByComparingTo("2000.00");
            assertThat(venta.getVendedor()).isEqualTo("ana");
            assertThat(venta.getItems()).singleElement().satisfies(item -> {
                assertThat(item.getProductoNombre()).isEqualTo("Laptop");
                assertThat(item.getCantidad()).isEqualTo(2);
                assertThat(item.getPrecioUnitario()).isEqualByComparingTo("1000.00");
                assertThat(item.getSubtotal()).isEqualByComparingTo("2000.00");
            });

            verify(inventoryMovementRepository).saveAll(movimientosCaptor.capture());
            assertThat(movimientosCaptor.getValue()).singleElement().satisfies(movimiento -> {
                assertThat(movimiento.getTipo()).isEqualTo(MovementType.VENTA);
                assertThat(movimiento.getCantidad()).isEqualTo(-2);
                assertThat(movimiento.getStockResultante()).isEqualTo(3);
            });
        }

        @Test
        @DisplayName("rechaza con 409 STOCK_INSUFICIENTE indicando disponible y solicitado")
        void stockInsuficiente() {
            prepararVenta();
            when(productRepository.decrementStock(eq(1L), eq(5), any())).thenReturn(0);
            when(productRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(producto(1L, "Laptop", "1000.00", 1)));

            assertThatThrownBy(() -> saleService.create(solicitud(linea(1L, 5)), null, VENDEDORA))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Stock insuficiente para 'Laptop': disponible 1, solicitado 5")
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.STOCK_INSUFICIENTE);

            verify(inventoryMovementRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("rechaza con 404 si el producto no existe o está inactivo")
        void productoInexistente() {
            prepararVenta();
            when(productRepository.decrementStock(eq(99L), eq(1), any())).thenReturn(0);
            when(productRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> saleService.create(solicitud(linea(99L, 1)), null, VENDEDORA))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Producto no encontrado con id: 99");
        }

        @Test
        @DisplayName("con una Idempotency-Key ya usada devuelve la venta original sin descontar stock")
        void idempotencia() {
            Sale original = Sale.builder().id(7L).codigo("VTA-20261112-ABCDEF12").usuario(ana).build();
            when(saleRepository.findByUsuarioIdAndClaveIdempotencia(2L, "clave-1")).thenReturn(Optional.of(original));
            when(saleRepository.findDetailById(7L)).thenReturn(Optional.of(original));

            SaleResponse venta = saleService.create(solicitud(linea(1L, 1)), "  clave-1  ", VENDEDORA);

            assertThat(venta.getCodigo()).isEqualTo("VTA-20261112-ABCDEF12");
            verify(productRepository, never()).decrementStock(anyLong(), anyInt(), any());
            verify(saleRepository, never()).save(any());
        }

        @Test
        @DisplayName("agrupa el mismo producto repetido y procesa los productos ordenados por id")
        void agrupaYOrdena() {
            prepararVenta();
            when(productRepository.decrementStock(anyLong(), anyInt(), any())).thenReturn(1);
            when(productRepository.findById(1L)).thenReturn(Optional.of(producto(1L, "Laptop", "1000.00", 4)));
            when(productRepository.findById(2L)).thenReturn(Optional.of(producto(2L, "Mouse", "25.50", 7)));

            SaleResponse venta = saleService.create(
                    solicitud(linea(2L, 1), linea(1L, 1), linea(2L, 2)), null, VENDEDORA);

            InOrder orden = inOrder(productRepository);
            orden.verify(productRepository).decrementStock(eq(1L), eq(1), any());
            orden.verify(productRepository).decrementStock(eq(2L), eq(3), any());
            assertThat(venta.getItems()).extracting("productoId", "cantidad")
                    .containsExactly(
                            tuple(1L, 1),
                            tuple(2L, 3));
            assertThat(venta.getTotal()).isEqualByComparingTo("1076.50");
        }

        @Test
        @DisplayName("rechaza con 400 una Idempotency-Key de más de 64 caracteres")
        void claveDemasiadoLarga() {
            assertThatThrownBy(() -> saleService.create(solicitud(linea(1L, 1)), "x".repeat(65), VENDEDORA))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.SOLICITUD_INVALIDA);
        }
    }

    @Nested
    @DisplayName("Consultar ventas")
    class Consultas {

        @Test
        @DisplayName("el ADMIN ve el historial completo")
        void adminVeTodas() {
            when(saleRepository.findAllByOrderByFechaCreacionDesc())
                    .thenReturn(List.of(venta(1L, ana), venta(2L, luis)));

            assertThat(saleService.findAll(ADMIN)).extracting("vendedor").containsExactly("ana", "luis");
            verify(saleRepository, never()).findAllByUsuarioIdOrderByFechaCreacionDesc(anyLong());
        }

        @Test
        @DisplayName("el VENDEDOR solo ve sus propias ventas")
        void vendedorVeLasSuyas() {
            when(saleRepository.findAllByUsuarioIdOrderByFechaCreacionDesc(2L)).thenReturn(List.of(venta(1L, ana)));

            assertThat(saleService.findAll(VENDEDORA)).extracting("vendedor").containsExactly("ana");
            verify(saleRepository, never()).findAllByOrderByFechaCreacionDesc();
        }

        @Test
        @DisplayName("un VENDEDOR recibe 404 al pedir una venta de otro vendedor")
        void ventaAjenaOculta() {
            when(saleRepository.findDetailById(5L)).thenReturn(Optional.of(venta(5L, luis)));

            assertThatThrownBy(() -> saleService.findById(5L, VENDEDORA))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Venta no encontrada con id: 5");
        }

        @Test
        @DisplayName("el ADMIN puede ver la venta de cualquier vendedor")
        void adminVeVentaAjena() {
            when(saleRepository.findDetailById(5L)).thenReturn(Optional.of(venta(5L, luis)));

            assertThat(saleService.findById(5L, ADMIN).getVendedor()).isEqualTo("luis");
        }
    }

    private void prepararVenta() {
        when(userRepository.getReferenceById(2L)).thenReturn(ana);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale sale = invocation.getArgument(0);
            sale.setId(10L);
            return sale;
        });
    }

    private static Product producto(Long id, String nombre, String precio, int stock) {
        return Product.builder().id(id).nombre(nombre).precio(new BigDecimal(precio)).stock(stock).build();
    }

    private static SaleItemRequest linea(Long productoId, int cantidad) {
        return SaleItemRequest.builder().productoId(productoId).cantidad(cantidad).build();
    }

    private static CreateSaleRequest solicitud(SaleItemRequest... lineas) {
        return CreateSaleRequest.builder().items(Arrays.asList(lineas)).build();
    }

    private static Sale venta(Long id, User vendedor) {
        return Sale.builder().id(id).codigo("VTA-" + id).usuario(vendedor).build();
    }
}
