package com.rememberme.api.service;

import com.rememberme.api.dto.request.LoginRequest;
import com.rememberme.api.dto.request.RegisterRequest;
import com.rememberme.api.dto.response.AuthResponse;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    public void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("admin-test@example.com");
        loginRequest.setPassword("Admin@123");
    }

    @Test
    public void register_AssignsUserRoleAutomatically() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(jwtUtil.generateToken("john@example.com")).thenReturn("jwtToken123");

        AuthResponse response = authService.register(registerRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(User.Role.USER, savedUser.getRole());
        assertEquals("USER", response.getRole());
        assertEquals("jwtToken123", response.getToken());
    }

    @Test
    public void login_AdminSuccess() {
        User adminUser = User.builder()
                .id(1L)
                .fullName("Admin Test")
                .email("admin-test@example.com")
                .password("encodedAdminPassword")
                .role(User.Role.ADMIN)
                .build();

        when(userRepository.findByEmail("admin-test@example.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("Admin@123", "encodedAdminPassword")).thenReturn(true);
        when(jwtUtil.generateToken("admin-test@example.com")).thenReturn("adminJwtToken");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("admin-test@example.com", response.getEmail());
        assertEquals("ADMIN", response.getRole());
        assertEquals("adminJwtToken", response.getToken());
        assertNotNull(response.getUser());
        assertEquals(1L, response.getUser().getId());
        assertEquals("Admin Test", response.getUser().getFullName());
        assertEquals("admin-test@example.com", response.getUser().getEmail());
        assertEquals("ADMIN", response.getUser().getRole());
    }

    @Test
    public void login_NormalUserSuccess() {
        User normalUser = User.builder()
                .id(2L)
                .fullName("Regular User")
                .email("user@example.com")
                .password("encodedUserPassword")
                .role(User.Role.USER)
                .build();

        LoginRequest userLoginRequest = new LoginRequest();
        userLoginRequest.setEmail("user@example.com");
        userLoginRequest.setPassword("User@123");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(normalUser));
        when(passwordEncoder.matches("User@123", "encodedUserPassword")).thenReturn(true);
        when(jwtUtil.generateToken("user@example.com")).thenReturn("userJwtToken");

        AuthResponse response = authService.login(userLoginRequest);

        assertNotNull(response);
        assertEquals("user@example.com", response.getEmail());
        assertEquals("USER", response.getRole());
        assertEquals("userJwtToken", response.getToken());
        assertNotNull(response.getUser());
        assertEquals(2L, response.getUser().getId());
        assertEquals("Regular User", response.getUser().getFullName());
        assertEquals("user@example.com", response.getUser().getEmail());
        assertEquals("USER", response.getUser().getRole());
    }

    @Test
    public void login_InvalidPassword_ThrowsException() {
        User adminUser = User.builder()
                .id(1L)
                .email("admin-test@example.com")
                .password("encodedAdminPassword")
                .role(User.Role.ADMIN)
                .build();

        when(userRepository.findByEmail("admin-test@example.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("Admin@123", "encodedAdminPassword")).thenReturn(false);

        assertThrows(ApiException.class, () -> authService.login(loginRequest));
    }
}
