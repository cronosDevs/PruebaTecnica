package com.example.demo.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9+/]+={0,2}$",
                message = "debe ser una clave Base64 de al menos 32 bytes; defina la variable de entorno JWT_SECRET")
        String secret,
        @Positive long expiration,
        @Positive long refreshExpiration,
        boolean refreshCookieSecure) {
}
