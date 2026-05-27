package com.codejudgex.auth.service;

import com.codejudgex.auth.dto.LoginRequest;
import com.codejudgex.auth.dto.RegisterRequest;
import com.codejudgex.auth.entity.Role;
import com.codejudgex.auth.entity.User;
import com.codejudgex.auth.repository.RefreshTokenRepository;
import com.codejudgex.auth.repository.RoleRepository;
import com.codejudgex.auth.repository.UserRepository;
import com.codejudgex.common.exception.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock RefreshTokenService refreshTokenService;

    @InjectMocks AuthService authService;

    private Role studentRole;
    private User existingUser;

    @BeforeEach
    void setUp() {
        studentRole = new Role("STUDENT");

        existingUser = new User();
        existingUser.setName("Test User");
        existingUser.setEmail("test@example.com");
        existingUser.setPasswordHash("$2a$10$hashed");
        existingUser.setStatus("ACTIVE");
        existingUser.getRoles().add(studentRole);
        // Simulate JPA-assigned ID
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(existingUser, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            // Simulate JPA assigning an ID on persist
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(u, UUID.randomUUID());
            return u;
        });
        when(jwtService.generateAccessToken(anyString(), any())).thenReturn("access-token");
        when(jwtService.getAccessTokenExpiryMs()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any())).thenReturn("refresh-token");

        var response = authService.register(request);

        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRoles()).contains("STUDENT");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsDuplicateResourceException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setName("Dup");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("correct-pass");

        when(userRepository.findByEmailWithRoles("test@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("correct-pass", "$2a$10$hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(anyString(), any())).thenReturn("access-token");
        when(jwtService.getAccessTokenExpiryMs()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any())).thenReturn("refresh-token");

        var response = authService.login(request);

        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getAccessToken()).isEqualTo("access-token");
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong-pass");

        when(userRepository.findByEmailWithRoles("test@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong-pass", "$2a$10$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_inactiveUser_throwsBadCredentials() {
        existingUser.setStatus("SUSPENDED");

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("correct-pass");

        when(userRepository.findByEmailWithRoles("test@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("correct-pass", "$2a$10$hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_userNotFound_throwsBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("pass");

        when(userRepository.findByEmailWithRoles("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
