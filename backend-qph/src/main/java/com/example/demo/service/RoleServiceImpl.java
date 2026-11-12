package com.example.demo.service;

import com.example.demo.dto.RoleResponse;
import com.example.demo.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public List<RoleResponse> findAll() {
        return roleRepository.findAllByOrderByIdAsc()
                .stream()
                .map(role -> RoleResponse.builder()
                        .id(role.getId())
                        .nombre(role.getNombre())
                        .descripcion(role.getDescripcion())
                        .build())
                .toList();
    }
}
