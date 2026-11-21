
package com.shopease.shopease.service;

import com.shopease.shopease.dto.AuthResponse;
import com.shopease.shopease.dto.LoginRequest;
import com.shopease.shopease.dto.RegisterRequest;
import com.shopease.shopease.dto.UserResponse;
import com.shopease.shopease.entity.Role;
import com.shopease.shopease.entity.User;
import com.shopease.shopease.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerSavesUserWithEncodedPasswordAndUserRole() {
        RegisterRequest request = new RegisterRequest("Ramya", "ramya@test.com", "secret123");
        when(userRepository.existsByEmail("ramya@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserResponse response = authService.register(request);

        assertEquals(1L, response.id());
        assertEquals(Role.USER, response.role());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("ENCODED", captor.getValue().getPassword());
    }

    @Test
    void registerWithExistingEmailThrowsConflict() {
        RegisterRequest request = new RegisterRequest("Ramya", "ramya@test.com", "secret123");
        when(userRepository.existsByEmail("ramya@test.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));

        assertEquals(409, ex.getStatusCode().value());
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = User.builder()
                .id(1L).name("Ramya").email("ramya@test.com")
                .password("ENCODED").role(Role.USER).build();
        when(userRepository.findByEmail("ramya@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("ramya@test.com", "USER")).thenReturn("fake-token");

        AuthResponse response = authService.login(new LoginRequest("ramya@test.com", "secret123"));

        assertEquals("fake-token", response.token());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void loginWithWrongPasswordThrowsUnauthorized() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("ramya@test.com", "wrong")));

        assertEquals(401, ex.getStatusCode().value());
        verify(jwtService, never()).generateToken(any(), any());
    }
}