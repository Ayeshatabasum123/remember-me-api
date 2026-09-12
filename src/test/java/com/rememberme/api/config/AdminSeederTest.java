package com.rememberme.api.config;

import com.rememberme.api.entity.User;
import com.rememberme.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminSeeder adminSeeder;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(adminSeeder, "adminEmail", "admin-test@example.com");
        ReflectionTestUtils.setField(adminSeeder, "adminPassword", "Admin@123");
    }

    @Test
    public void run_CreatesAdmin_WhenAdminDoesNotExist() {
        when(userRepository.findByEmail("admin-test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Admin@123")).thenReturn("encodedPassword123");

        adminSeeder.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("Admin Test", savedUser.getFullName());
        assertEquals("admin-test@example.com", savedUser.getEmail());
        assertEquals("encodedPassword123", savedUser.getPassword());
        assertEquals(User.Role.ADMIN, savedUser.getRole());
        assertTrue(savedUser.isEnabled());
        assertTrue(savedUser.isEmailVerified());
    }

    @Test
    public void run_DoesNotDuplicateAdmin_WhenAdminAlreadyExists() {
        User existingAdmin = User.builder()
                .id(1L)
                .email("admin-test@example.com")
                .role(User.Role.ADMIN)
                .build();
        when(userRepository.findByEmail("admin-test@example.com")).thenReturn(Optional.of(existingAdmin));

        adminSeeder.run();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }
}
