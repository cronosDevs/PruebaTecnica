package com.example.demo.dto;

public record AuthSession(AuthResponse body, String refreshToken) {
}
