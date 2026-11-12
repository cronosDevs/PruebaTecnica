package com.example.demo.service;

import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.UpdateUserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAllByOrderByIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(findUser(id));
    }

    @Override
    public UserResponse create(CreateUserRequest request, UserPrincipal actor) {
        String username = normalize(request.getUsername());
        String email = normalize(request.getEmail());

        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.RECURSO_DUPLICADO,
                    "Ya existe un usuario con el username '%s'".formatted(username));
        }
        if (userRepository.existsByEmail(email)) {
            throw duplicatedEmail(email);
        }

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nombreCompleto(request.getNombreCompleto().trim())
                .email(email)
                .rol(findRole(request.getRol()))
                .build();

        User saved = userRepository.save(user);
        log.info("Usuario creado: id={}, username={}, rol={}, creadoPor={}",
                saved.getId(), saved.getUsername(), saved.getRol().getNombre(), actor.getUsername());
        return toResponse(saved);
    }

    @Override
    public UserResponse update(Long id, UpdateUserRequest request, UserPrincipal actor) {
        User user = findUser(id);
        String email = normalize(request.getEmail());
        Role rol = findRole(request.getRol());

        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw duplicatedEmail(email);
        }
        if (isSelf(user, actor) && !Role.ADMIN.equals(rol.getNombre())) {
            throw new BusinessException(ErrorCode.OPERACION_NO_PERMITIDA,
                    "No puede quitarse a sí mismo el rol de administrador");
        }

        user.setNombreCompleto(request.getNombreCompleto().trim());
        user.setEmail(email);
        user.setRol(rol);
        boolean passwordChanged = request.getPassword() != null;
        if (passwordChanged) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        User saved = userRepository.save(user);
        if (passwordChanged) {
            refreshTokenService.revokeAll(saved.getId());
        }
        log.info("Usuario actualizado: id={}, username={}, rol={}, cambioPassword={}, actualizadoPor={}",
                saved.getId(), saved.getUsername(), rol.getNombre(), passwordChanged, actor.getUsername());
        return toResponse(saved);
    }

    @Override
    public UserResponse changeStatus(Long id, boolean activo, UserPrincipal actor) {
        User user = findUser(id);

        if (isSelf(user, actor) && !activo) {
            throw new BusinessException(ErrorCode.OPERACION_NO_PERMITIDA,
                    "No puede desactivar su propia cuenta");
        }

        user.setActivo(activo);
        User saved = userRepository.save(user);
        if (!activo) {
            refreshTokenService.revokeAll(saved.getId());
        }
        log.info("Usuario {}: id={}, username={}, por={}",
                activo ? "activado" : "desactivado", saved.getId(), saved.getUsername(), actor.getUsername());
        return toResponse(saved);
    }

    private User findUser(Long id) {
        return userRepository.findWithRolById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    private Role findRole(String nombre) {
        return roleRepository.findByNombre(nombre.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException(ErrorCode.SOLICITUD_INVALIDA,
                        "El rol '%s' no existe".formatted(nombre)));
    }

    private static boolean isSelf(User user, UserPrincipal actor) {
        return user.getId().equals(actor.getId());
    }

    private static BusinessException duplicatedEmail(String email) {
        return new BusinessException(ErrorCode.RECURSO_DUPLICADO,
                "Ya existe un usuario con el email '%s'".formatted(email));
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nombreCompleto(user.getNombreCompleto())
                .email(user.getEmail())
                .rol(user.getRol().getNombre())
                .activo(user.getActivo())
                .fechaCreacion(user.getFechaCreacion())
                .fechaActualizacion(user.getFechaActualizacion())
                .build();
    }
}
