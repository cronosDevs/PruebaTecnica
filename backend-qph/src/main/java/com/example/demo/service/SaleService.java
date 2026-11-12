package com.example.demo.service;

import com.example.demo.dto.CreateSaleRequest;
import com.example.demo.dto.SaleResponse;
import com.example.demo.security.UserPrincipal;

import java.util.List;

public interface SaleService {

    SaleResponse create(CreateSaleRequest request, String idempotencyKey, UserPrincipal actor);

    List<SaleResponse> findAll(UserPrincipal actor);

    SaleResponse findById(Long id, UserPrincipal actor);
}
