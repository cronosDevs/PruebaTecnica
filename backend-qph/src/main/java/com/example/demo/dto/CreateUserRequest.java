package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    @NotBlank(message = "El usuario es requerido")
    @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$",
            message = "El usuario solo puede contener letras, números, punto, guion y guion bajo")
    private String username;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "La contraseña debe tener al menos una letra y un número")
    private String password;

    @NotBlank(message = "El nombre completo es requerido")
    @Size(max = 120, message = "El nombre completo no puede superar 120 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 150, message = "El email no puede superar 150 caracteres")
    private String email;

    @NotBlank(message = "El rol es requerido")
    private String rol;
}
