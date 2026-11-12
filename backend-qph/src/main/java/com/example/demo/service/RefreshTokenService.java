package com.example.demo.service;

public interface RefreshTokenService {

    String create(Long usuarioId);

    RefreshRotation rotate(String token);

    void revoke(String token);

    void revokeAll(Long usuarioId);
}
