package com.example.demo.controller;

import com.example.demo.dto.RoleResponse;
import com.example.demo.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Roles", description = "Catálogo de roles. Solo ADMIN")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "Listar roles disponibles")
    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAll() {
        return ResponseEntity.ok(roleService.findAll());
    }
}
