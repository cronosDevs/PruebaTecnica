package com.example.demo.service;

import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.UpdateUserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.security.UserPrincipal;

import java.util.List;

public interface UserService {

    List<UserResponse> findAll();

    UserResponse findById(Long id);

    UserResponse create(CreateUserRequest request, UserPrincipal actor);

    UserResponse update(Long id, UpdateUserRequest request, UserPrincipal actor);

    UserResponse changeStatus(Long id, boolean activo, UserPrincipal actor);
}
