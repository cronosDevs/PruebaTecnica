package com.example.demo;

import com.example.demo.dto.CreateSaleRequest;
import com.example.demo.dto.SaleItemRequest;
import com.example.demo.dto.SaleResponse;
import com.example.demo.entity.Product;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.SaleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaleIntegrationTest extends IntegrationTest {

    @Autowired
    private SaleService saleService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UserPrincipal admin;

    @BeforeEach
    void setUp() {
        admin = UserPrincipal.from(userRepository.findByUsername("admin").orElseThrow());
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE sale_items, inventory_movements, sales, products RESTART IDENTITY CASCADE");
    }

    @Test
    @DisplayName("10 ventas simultáneas de un producto con stock 5: solo 5 se registran y el stock nunca queda negativo")
    void ventasSimultaneasNoVendenMasDelStock() throws Exception {
        Long consolaId = crearProducto("Consola", "500.00", 5);
        int intentos = 10;
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(intentos);

        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < intentos; i++) {
            resultados.add(hilos.submit(intentarVenta(largada, consolaId)));
        }
        largada.countDown();

        int exitosas = 0;
        for (Future<Boolean> resultado : resultados) {
            if (resultado.get()) {
                exitosas++;
            }
        }
        hilos.shutdown();

        assertThat(exitosas).isEqualTo(5);
        assertThat(productRepository.findById(consolaId).orElseThrow().getStock()).isZero();
        assertThat(contar("SELECT count(*) FROM sale_items WHERE producto_id = ?", consolaId)).isEqualTo(5);
        assertThat(jdbcTemplate.queryForList(
                "SELECT stock_resultante FROM inventory_movements WHERE producto_id = ? AND tipo = 'VENTA' ORDER BY stock_resultante DESC",
                Integer.class, consolaId))
                .containsExactly(4, 3, 2, 1, 0);
    }

    @Test
    @DisplayName("si un producto de la venta no tiene stock se revierte la venta completa")
    void ventaSinStockRevierteTodo() {
        Long mouseId = crearProducto("Mouse", "25.50", 10);
        Long laptopId = crearProducto("Laptop", "1000.00", 1);

        assertThatThrownBy(() -> saleService.create(venta(linea(mouseId, 2), linea(laptopId, 5)), null, admin))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STOCK_INSUFICIENTE);

        assertThat(productRepository.findById(mouseId).orElseThrow().getStock()).isEqualTo(10);
        assertThat(productRepository.findById(laptopId).orElseThrow().getStock()).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM sales")).isZero();
        assertThat(contar("SELECT count(*) FROM inventory_movements WHERE tipo = 'VENTA'")).isZero();
    }

    @Test
    @DisplayName("repetir la venta con la misma Idempotency-Key no descuenta el stock dos veces")
    void idempotenciaContraLaBaseDeDatos() {
        Long mouseId = crearProducto("Mouse", "25.50", 10);

        SaleResponse primera = saleService.create(venta(linea(mouseId, 3)), "clave-unica-1", admin);
        SaleResponse repetida = saleService.create(venta(linea(mouseId, 3)), "clave-unica-1", admin);

        assertThat(repetida.getId()).isEqualTo(primera.getId());
        assertThat(repetida.getTotal()).isEqualByComparingTo("76.50");
        assertThat(productRepository.findById(mouseId).orElseThrow().getStock()).isEqualTo(7);
        assertThat(contar("SELECT count(*) FROM sales")).isEqualTo(1);
    }

    @Test
    @DisplayName("la venta conserva el precio del momento aunque luego cambie el precio del producto")
    void precioCongelado() {
        Long laptopId = crearProducto("Laptop", "1000.00", 5);
        SaleResponse venta = saleService.create(venta(linea(laptopId, 1)), null, admin);

        Product laptop = productRepository.findById(laptopId).orElseThrow();
        laptop.setPrecio(new BigDecimal("1500.00"));
        productRepository.save(laptop);

        assertThat(saleService.findById(venta.getId(), admin).getItems().get(0).getPrecioUnitario())
                .isEqualByComparingTo("1000.00");
    }

    private Callable<Boolean> intentarVenta(CountDownLatch largada, Long productoId) {
        return () -> {
            largada.await();
            try {
                saleService.create(venta(linea(productoId, 1)), null, admin);
                return true;
            } catch (BusinessException ex) {
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STOCK_INSUFICIENTE);
                return false;
            }
        };
    }

    private Long crearProducto(String nombre, String precio, int stock) {
        return productRepository.save(Product.builder()
                .nombre(nombre).precio(new BigDecimal(precio)).stock(stock).build()).getId();
    }

    private int contar(String sql, Object... parametros) {
        Integer total = jdbcTemplate.queryForObject(sql, Integer.class, parametros);
        return total == null ? 0 : total;
    }

    private static SaleItemRequest linea(Long productoId, int cantidad) {
        return SaleItemRequest.builder().productoId(productoId).cantidad(cantidad).build();
    }

    private static CreateSaleRequest venta(SaleItemRequest... lineas) {
        return CreateSaleRequest.builder().items(List.of(lineas)).build();
    }
}
