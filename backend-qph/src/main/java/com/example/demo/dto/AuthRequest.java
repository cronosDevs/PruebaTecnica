package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {

    @NotBlank(message = "El usuario es requerido")
    private String username;

    @NotBlank(message = "La contraseña es requerida")
    private String password;
}
