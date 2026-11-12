package com.example.demo.service;

import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.AuthSession;

public interface AuthService {

    AuthSession login(AuthRequest request);

    AuthSession refresh(String refreshToken);

    void logout(String refreshToken);
}
