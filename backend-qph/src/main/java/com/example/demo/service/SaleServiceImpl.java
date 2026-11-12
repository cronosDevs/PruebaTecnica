package com.example.demo.service;

import com.example.demo.dto.CreateSaleRequest;
import com.example.demo.dto.SaleItemRequest;
import com.example.demo.dto.SaleItemResponse;
import com.example.demo.dto.SaleResponse;
import com.example.demo.entity.InventoryMovement;
import com.example.demo.entity.MovementType;
import com.example.demo.entity.Product;
import com.example.demo.entity.Role;
import com.example.demo.entity.Sale;
import com.example.demo.entity.SaleItem;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.InventoryMovementRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SaleServiceImpl implements SaleService {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 64;
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    @Override
    public SaleResponse create(CreateSaleRequest request, String idempotencyKey, UserPrincipal actor) {
        String clave = normalizeKey(idempotencyKey);

        if (clave != null) {
            Optional<Sale> previa = saleRepository.findByUsuarioIdAndClaveIdempotencia(actor.getId(), clave);
            if (previa.isPresent()) {
                log.info("Venta repetida con la misma Idempotency-Key, se devuelve la original: codigo={}, vendedor={}",
                        previa.get().getCodigo(), actor.getUsername());
                return toResponse(findSale(previa.get().getId()));
            }
        }

        User vendedor = userRepository.getReferenceById(actor.getId());
        OffsetDateTime ahora = OffsetDateTime.now();

        Sale sale = saleRepository.save(Sale.builder()
                .codigo(generateCode())
                .usuario(vendedor)
                .claveIdempotencia(clave)
                .build());

        List<InventoryMovement> movimientos = new ArrayList<>();
        for (Map.Entry<Long, Integer> linea : groupByProduct(request.getItems()).entrySet()) {
            Product producto = discountStock(linea.getKey(), linea.getValue(), ahora);
            sale.addItem(SaleItem.of(producto, linea.getValue()));
            movimientos.add(InventoryMovement.builder()
                    .producto(producto)
                    .venta(sale)
                    .usuario(vendedor)
                    .tipo(MovementType.VENTA)
                    .cantidad(-linea.getValue())
                    .stockResultante(producto.getStock())
                    .build());
        }
        inventoryMovementRepository.saveAll(movimientos);

        log.info("Venta registrada: codigo={}, vendedor={}, productos={}, total={}",
                sale.getCodigo(), actor.getUsername(), sale.getItems().size(), sale.getTotal());
        return toResponse(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleResponse> findAll(UserPrincipal actor) {
        List<Sale> ventas = isAdmin(actor)
                ? saleRepository.findAllByOrderByFechaCreacionDesc()
                : saleRepository.findAllByUsuarioIdOrderByFechaCreacionDesc(actor.getId());
        return ventas.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SaleResponse findById(Long id, UserPrincipal actor) {
        Sale sale = findSale(id);
        if (!isAdmin(actor) && !sale.getUsuario().getId().equals(actor.getId())) {
            throw saleNotFound(id);
        }
        return toResponse(sale);
    }

    private Product discountStock(Long productoId, int cantidad, OffsetDateTime ahora) {
        if (productRepository.decrementStock(productoId, cantidad, ahora) == 0) {
            Product producto = productRepository.findByIdAndActivoTrue(productoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
            throw new BusinessException(ErrorCode.STOCK_INSUFICIENTE,
                    "Stock insuficiente para '%s': disponible %d, solicitado %d"
                            .formatted(producto.getNombre(), producto.getStock(), cantidad));
        }
        return productRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
    }

    private static Map<Long, Integer> groupByProduct(List<SaleItemRequest> items) {
        Map<Long, Integer> cantidades = new TreeMap<>();
        for (SaleItemRequest item : items) {
            cantidades.merge(item.getProductoId(), item.getCantidad(), Integer::sum);
        }
        return cantidades;
    }

    private Sale findSale(Long id) {
        return saleRepository.findDetailById(id).orElseThrow(() -> saleNotFound(id));
    }

    private static ResourceNotFoundException saleNotFound(Long id) {
        return new ResourceNotFoundException("Venta no encontrada con id: " + id);
    }

    private static boolean isAdmin(UserPrincipal actor) {
        return Role.ADMIN.equals(actor.getRol());
    }

    private static String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String clave = idempotencyKey.trim();
        if (clave.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new BusinessException(ErrorCode.SOLICITUD_INVALIDA,
                    "El header Idempotency-Key no puede superar %d caracteres".formatted(MAX_IDEMPOTENCY_KEY_LENGTH));
        }
        return clave;
    }

    private static String generateCode() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return "VTA-" + LocalDate.now().format(CODE_DATE) + "-" + sufijo;
    }

    private SaleResponse toResponse(Sale sale) {
        return SaleResponse.builder()
                .id(sale.getId())
                .codigo(sale.getCodigo())
                .total(sale.getTotal())
                .vendedor(sale.getUsuario().getUsername())
                .vendedorNombre(sale.getUsuario().getNombreCompleto())
                .fechaCreacion(sale.getFechaCreacion())
                .items(sale.getItems().stream()
                        .map(item -> SaleItemResponse.builder()
                                .productoId(item.getProducto().getId())
                                .productoNombre(item.getProducto().getNombre())
                                .cantidad(item.getCantidad())
                                .precioUnitario(item.getPrecioUnitario())
                                .subtotal(item.getSubtotal())
                                .build())
                        .toList())
                .build();
    }
}
