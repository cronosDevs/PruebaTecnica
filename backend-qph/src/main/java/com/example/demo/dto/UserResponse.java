package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String nombreCompleto;
    private String email;
    private String rol;
    private Boolean activo;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;
}
