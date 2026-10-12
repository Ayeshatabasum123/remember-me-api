package com.rememberme.api.service;

import com.rememberme.api.dto.request.LoginRequest;
import com.rememberme.api.dto.request.RegisterRequest;
import com.rememberme.api.dto.request.UserPostcodeRequest;
import com.rememberme.api.dto.response.AuthResponse;
import com.rememberme.api.dto.response.PostcodeValidationResponse;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthAndUserPostcodeTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PostcodeService postcodeService;

    private AuthService authService;

    @BeforeEach
    public void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtUtil,
                null,
                null,
                null,
                null
        );
    }

    @Test
    public void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_pass");
        when(jwtUtil.generateToken("john@example.com")).thenReturn("mock_token");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(10L, response.getUserId());
        assertEquals("mock_token", response.getToken());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("John Doe", response.getFullName());
        assertEquals("USER", response.getRole());
    }

    @Test
    public void register_DuplicateEmail_ThrowsBadRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("duplicate@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("already registered"));
    }

    @Test
    public void register_PasswordMismatch_ThrowsBadRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("mismatched123");

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Password and Confirm Password must match"));
    }

    @Test
    public void register_ShortPassword_ThrowsBadRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("short");
        request.setConfirmPassword("short");

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    public void login_Success() {
        User user = User.builder()
                .id(5L)
                .email("user@example.com")
                .fullName("Test User")
                .password("hashed_password")
                .role(User.Role.USER)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw_password", "hashed_password")).thenReturn(true);
        when(jwtUtil.generateToken("user@example.com")).thenReturn("token_123");

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("raw_password");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals(5L, response.getUserId());
        assertEquals("token_123", response.getToken());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("USER", response.getRole());
    }

    @Test
    public void login_WrongPassword_ThrowsUnauthorized() {
        User user = User.builder()
                .id(5L)
                .email("user@example.com")
                .password("hashed_password")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrong_password");

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }
}
