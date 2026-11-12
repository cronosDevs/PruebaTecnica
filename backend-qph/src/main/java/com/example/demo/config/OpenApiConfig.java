package com.example.demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI productHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ProductHub API")
                        .version("1.0.0")
                        .description("""
                                API de gestión de productos, usuarios y ventas.

                                Autenticación: obtenga un token con POST /api/auth/login y envíelo en el header \
                                Authorization: Bearer <token>.

                                Roles: ADMIN administra productos y usuarios y ve todas las ventas. \
                                VENDEDOR consulta productos, registra ventas y ve solo las propias.

                                Errores: todas las respuestas de error usan el formato Problem Details (RFC 9457) \
                                con los campos adicionales codigo, fecha y requestId."""))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
