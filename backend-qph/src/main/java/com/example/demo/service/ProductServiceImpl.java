package com.example.demo.service;

import com.example.demo.dto.InventoryMovementResponse;
import com.example.demo.dto.ProductDTO;
import com.example.demo.entity.InventoryMovement;
import com.example.demo.entity.MovementType;
import com.example.demo.entity.Product;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.InventoryMovementRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> findAll() {
        return repository.findAllByActivoTrueOrderByIdAsc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO findById(Long id) {
        return toDTO(findActive(id));
    }

    @Override
    public ProductDTO create(ProductDTO dto, UserPrincipal actor) {
        Product saved = repository.save(toEntity(dto));
        if (saved.getStock() > 0) {
            registerMovement(saved, MovementType.ENTRADA, saved.getStock(), actor);
        }
        log.info("Producto creado: id={}, nombre={}, precio={}, stock={}, creadoPor={}",
                saved.getId(), saved.getNombre(), saved.getPrecio(), saved.getStock(), actor.getUsername());
        return toDTO(saved);
    }

    @Override
    public ProductDTO update(Long id, ProductDTO dto, UserPrincipal actor) {
        Product existing = findActive(id);
        int stockAnterior = existing.getStock();
        BigDecimal precioAnterior = existing.getPrecio();
        existing.setNombre(dto.getNombre());
        existing.setPrecio(dto.getPrecio());
        existing.setStock(dto.getStock());
        Product saved = repository.save(existing);

        int diferencia = saved.getStock() - stockAnterior;
        if (diferencia != 0) {
            registerMovement(saved, MovementType.AJUSTE, diferencia, actor);
        }
        log.info("Producto actualizado: id={}, precio={} -> {}, stock={} -> {}, actualizadoPor={}",
                saved.getId(), precioAnterior, saved.getPrecio(), stockAnterior, saved.getStock(), actor.getUsername());
        return toDTO(saved);
    }

    @Override
    public void delete(Long id, UserPrincipal actor) {
        Product product = findActive(id);
        product.setActivo(false);
        repository.save(product);
        log.info("Producto eliminado (borrado lógico): id={}, nombre={}, eliminadoPor={}",
                product.getId(), product.getNombre(), actor.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> findMovements(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado con id: " + id);
        }
        return inventoryMovementRepository.findByProductoIdOrderByFechaDescIdDesc(id)
                .stream()
                .map(m -> InventoryMovementResponse.builder()
                        .id(m.getId())
                        .tipo(m.getTipo().name())
                        .cantidad(m.getCantidad())
                        .stockResultante(m.getStockResultante())
                        .fecha(m.getFecha())
                        .usuario(m.getUsuario().getUsername())
                        .ventaCodigo(m.getVenta() != null ? m.getVenta().getCodigo() : null)
                        .build())
                .toList();
    }

    private void registerMovement(Product product, MovementType tipo, int cantidad, UserPrincipal actor) {
        inventoryMovementRepository.save(InventoryMovement.builder()
                .producto(product)
                .usuario(userRepository.getReferenceById(actor.getId()))
                .tipo(tipo)
                .cantidad(cantidad)
                .stockResultante(product.getStock())
                .build());
    }

    private Product findActive(Long id) {
        return repository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado con id: " + id));
    }

    private ProductDTO toDTO(Product p) {
        return ProductDTO.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .precio(p.getPrecio())
                .stock(p.getStock())
                .build();
    }

    private Product toEntity(ProductDTO dto) {
        return Product.builder()
                .nombre(dto.getNombre())
                .precio(dto.getPrecio())
                .stock(dto.getStock())
                .build();
    }
}
