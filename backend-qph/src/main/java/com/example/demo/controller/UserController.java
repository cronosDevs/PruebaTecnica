package com.example.demo.controller;

import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.UpdateUserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.dto.UserStatusRequest;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Usuarios", description = "Gestión de usuarios. Solo ADMIN")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Listar usuarios", description = "Incluye usuarios activos y desactivados.")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @Operation(summary = "Obtener un usuario")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado")
    @ApiResponse(responseCode = "404", description = "El usuario no existe")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Operation(summary = "Crear un usuario",
            description = "El username y el email se guardan en minúsculas. La contraseña se almacena con BCrypt.")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o rol inexistente")
    @ApiResponse(responseCode = "409", description = "El username o el email ya existen")
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request,
                                               @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal actor) {
        UserResponse created = userService.create(request, actor);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Editar un usuario",
            description = "Actualiza nombre, email y rol. La contraseña solo cambia si se envía.")
    @ApiResponse(responseCode = "200", description = "Usuario actualizado")
    @ApiResponse(responseCode = "404", description = "El usuario no existe")
    @ApiResponse(responseCode = "409", description = "El email pertenece a otro usuario")
    @ApiResponse(responseCode = "422", description = "Un administrador intentó quitarse su propio rol")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateUserRequest request,
                                               @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal actor) {
        return ResponseEntity.ok(userService.update(id, request, actor));
    }

    @Operation(summary = "Activar o desactivar un usuario",
            description = "Un usuario desactivado no puede iniciar sesión y sus tokens dejan de ser válidos de inmediato.")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "404", description = "El usuario no existe")
    @ApiResponse(responseCode = "422", description = "Un administrador intentó desactivarse a sí mismo")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<UserResponse> changeStatus(@PathVariable Long id,
                                                     @Valid @RequestBody UserStatusRequest request,
                                                     @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal actor) {
        return ResponseEntity.ok(userService.changeStatus(id, request.getActivo(), actor));
    }
}
