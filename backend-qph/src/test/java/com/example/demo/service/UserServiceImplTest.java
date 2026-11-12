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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final UserPrincipal ADMIN =
            new UserPrincipal(1L, "admin", "hash", "Administrador", Role.ADMIN, true);

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private final Role rolAdmin = Role.builder().id(1).nombre(Role.ADMIN).build();
    private final Role rolVendedor = Role.builder().id(2).nombre(Role.VENDEDOR).build();

    @Nested
    @DisplayName("Crear usuario")
    class Create {

        @Test
        @DisplayName("normaliza username y email, limpia el nombre y guarda la contraseña cifrada")
        void creaUsuario() {
            when(roleRepository.findByNombre("VENDEDOR")).thenReturn(Optional.of(rolVendedor));
            when(passwordEncoder.encode("Ventas2026")).thenReturn("hash-bcrypt");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(5L);
                return user;
            });

            UserResponse creado = userService.create(CreateUserRequest.builder()
                    .username("Carlos.Ruiz")
                    .password("Ventas2026")
                    .nombreCompleto("  Carlos Ruiz  ")
                    .email("Carlos.Ruiz@Mail.com")
                    .rol("vendedor")
                    .build(), ADMIN);

            assertThat(creado.getId()).isEqualTo(5L);
            assertThat(creado.getUsername()).isEqualTo("carlos.ruiz");
            assertThat(creado.getEmail()).isEqualTo("carlos.ruiz@mail.com");
            assertThat(creado.getNombreCompleto()).isEqualTo("Carlos Ruiz");
            assertThat(creado.getRol()).isEqualTo("VENDEDOR");
            assertThat(creado.getActivo()).isTrue();

            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hash-bcrypt");
        }

        @Test
        @DisplayName("rechaza con 409 un username existente aunque cambien las mayúsculas")
        void usernameDuplicado() {
            when(userRepository.existsByUsername("carlos.ruiz")).thenReturn(true);

            assertThatThrownBy(() -> userService.create(solicitud("CARLOS.RUIZ", "otro@mail.com", "VENDEDOR"), ADMIN))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Ya existe un usuario con el username 'carlos.ruiz'")
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.RECURSO_DUPLICADO);

            verify(userRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("rechaza con 409 un email existente")
        void emailDuplicado() {
            when(userRepository.existsByEmail("carlos.ruiz@mail.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.create(solicitud("otro", "Carlos.Ruiz@Mail.com", "VENDEDOR"), ADMIN))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Ya existe un usuario con el email 'carlos.ruiz@mail.com'");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza con 400 un rol inexistente")
        void rolInexistente() {
            when(roleRepository.findByNombre("GERENTE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.create(solicitud("pepe", "pepe@mail.com", "GERENTE"), ADMIN))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("El rol 'GERENTE' no existe")
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.SOLICITUD_INVALIDA);
        }

        private CreateUserRequest solicitud(String username, String email, String rol) {
            return CreateUserRequest.builder()
                    .username(username).password("Clave1234").nombreCompleto("Prueba").email(email).rol(rol)
                    .build();
        }
    }

    @Nested
    @DisplayName("Editar usuario")
    class Update {

        @Test
        @DisplayName("sin contraseña conserva el hash actual")
        void sinPasswordConservaHash() {
            User carlos = usuario(2L, rolVendedor);
            when(userRepository.findWithRolById(2L)).thenReturn(Optional.of(carlos));
            when(roleRepository.findByNombre("ADMIN")).thenReturn(Optional.of(rolAdmin));
            when(userRepository.save(carlos)).thenReturn(carlos);

            UserResponse editado = userService.update(2L, edicion("Nuevo@Mail.com", "ADMIN", null), ADMIN);

            assertThat(editado.getEmail()).isEqualTo("nuevo@mail.com");
            assertThat(editado.getRol()).isEqualTo("ADMIN");
            assertThat(carlos.getPasswordHash()).isEqualTo("hash-viejo");
            verify(passwordEncoder, never()).encode(any());
            verify(refreshTokenService, never()).revokeAll(any());
        }

        @Test
        @DisplayName("con contraseña la cifra, la reemplaza y cierra las sesiones abiertas del usuario")
        void conPasswordLaCambia() {
            User carlos = usuario(2L, rolVendedor);
            when(userRepository.findWithRolById(2L)).thenReturn(Optional.of(carlos));
            when(roleRepository.findByNombre("VENDEDOR")).thenReturn(Optional.of(rolVendedor));
            when(passwordEncoder.encode("Nueva2026")).thenReturn("hash-nuevo");
            when(userRepository.save(carlos)).thenReturn(carlos);

            userService.update(2L, edicion("carlos@mail.com", "VENDEDOR", "Nueva2026"), ADMIN);

            assertThat(carlos.getPasswordHash()).isEqualTo("hash-nuevo");
            verify(refreshTokenService).revokeAll(2L);
        }

        @Test
        @DisplayName("rechaza con 409 el email de otro usuario")
        void emailDeOtroUsuario() {
            when(userRepository.findWithRolById(2L)).thenReturn(Optional.of(usuario(2L, rolVendedor)));
            when(roleRepository.findByNombre("VENDEDOR")).thenReturn(Optional.of(rolVendedor));
            when(userRepository.existsByEmailAndIdNot("admin@producthub.local", 2L)).thenReturn(true);

            assertThatThrownBy(() -> userService.update(2L, edicion("admin@producthub.local", "VENDEDOR", null), ADMIN))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.RECURSO_DUPLICADO);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("un ADMIN no puede quitarse su propio rol (422)")
        void adminNoSeQuitaElRol() {
            when(userRepository.findWithRolById(1L)).thenReturn(Optional.of(usuario(1L, rolAdmin)));
            when(roleRepository.findByNombre("VENDEDOR")).thenReturn(Optional.of(rolVendedor));

            assertThatThrownBy(() -> userService.update(1L, edicion("admin@producthub.local", "VENDEDOR", null), ADMIN))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("No puede quitarse a sí mismo el rol de administrador")
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.OPERACION_NO_PERMITIDA);

            verify(userRepository, never()).save(any());
        }

        private UpdateUserRequest edicion(String email, String rol, String password) {
            return UpdateUserRequest.builder()
                    .nombreCompleto("Carlos A. Ruiz").email(email).rol(rol).password(password)
                    .build();
        }
    }

    @Nested
    @DisplayName("Activar y desactivar")
    class ChangeStatus {

        @Test
        @DisplayName("desactiva a otro usuario y cierra sus sesiones")
        void desactivaOtro() {
            User carlos = usuario(2L, rolVendedor);
            when(userRepository.findWithRolById(2L)).thenReturn(Optional.of(carlos));
            when(userRepository.save(carlos)).thenReturn(carlos);

            assertThat(userService.changeStatus(2L, false, ADMIN).getActivo()).isFalse();
            verify(refreshTokenService).revokeAll(2L);
        }

        @Test
        @DisplayName("un ADMIN no puede desactivarse a sí mismo (422)")
        void noSeDesactivaASiMismo() {
            when(userRepository.findWithRolById(1L)).thenReturn(Optional.of(usuario(1L, rolAdmin)));

            assertThatThrownBy(() -> userService.changeStatus(1L, false, ADMIN))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("No puede desactivar su propia cuenta");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("responde 404 si el usuario no existe")
        void usuarioInexistente() {
            when(userRepository.findWithRolById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changeStatus(99L, false, ADMIN))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Usuario no encontrado con id: 99");
        }
    }

    private static User usuario(Long id, Role rol) {
        return User.builder()
                .id(id).username("usuario" + id).passwordHash("hash-viejo")
                .nombreCompleto("Usuario " + id).email("usuario" + id + "@mail.com").rol(rol)
                .build();
    }
}
