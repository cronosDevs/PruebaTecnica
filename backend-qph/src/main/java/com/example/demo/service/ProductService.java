package com.example.demo.service;

import com.example.demo.dto.InventoryMovementResponse;
import com.example.demo.dto.ProductDTO;
import com.example.demo.security.UserPrincipal;

import java.util.List;

public interface ProductService {

    List<ProductDTO> findAll();

    ProductDTO findById(Long id);

    ProductDTO create(ProductDTO dto, UserPrincipal actor);

    ProductDTO update(Long id, ProductDTO dto, UserPrincipal actor);

    void delete(Long id, UserPrincipal actor);

    List<InventoryMovementResponse> findMovements(Long id);
}
