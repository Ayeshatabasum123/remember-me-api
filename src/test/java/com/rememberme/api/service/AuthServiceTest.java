package com.rememberme.api.service;

import com.rememberme.api.dto.request.LoginRequest;
import com.rememberme.api.dto.request.RegisterRequest;
import com.rememberme.api.dto.response.AuthResponse;
import com.rememberme.api.entity.Organization;
import com.rememberme.api.entity.OrganizationAdmin;
import com.rememberme.api.entity.OrganizationApprovalStatus;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.OrganizationAdminRepository;
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

    @Mock
    private OrganizationAdminRepository organizationAdminRepository;

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
        registerRequest.setConfirmPassword("password123");

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
    public void register_PasswordShorterThan8Chars_ThrowsApiException() {
        registerRequest.setPassword("1234567");
        registerRequest.setConfirmPassword("1234567");

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(registerRequest));
        assertEquals("Password must be at least 8 characters long.", ex.getMessage());
    }

    @Test
    public void register_PasswordAndConfirmPasswordMismatch_ThrowsApiException() {
        registerRequest.setPassword("password123");
        registerRequest.setConfirmPassword("different123");

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(registerRequest));
        assertEquals("Password and Confirm Password must match.", ex.getMessage());
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
        assertNull(response.getOrganizationName());
        assertNull(response.getOrganizationCode());
        assertNull(response.getPostcode());
        assertNull(response.getApprovalStatus());
    }

    @Test
    public void login_AdminWithOrganization_ReturnsOrganizationDetails() {
        User adminUser = User.builder()
                .id(10L)
                .fullName("Westminster Admin")
                .email("admin@westminster.org")
                .password("encodedAdminPass")
                .role(User.Role.ADMIN)
                .postcode("SW1A 1AA")
                .build();

        Organization organization = Organization.builder()
                .id(2L)
                .name("Westminster Community Church")
                .organizationCode("ORG-000002")
                .postcode("SW1A 1AA")
                .approvalStatus(OrganizationApprovalStatus.APPROVED)
                .build();

        OrganizationAdmin orgAdmin = OrganizationAdmin.builder()
                .id(1L)
                .user(adminUser)
                .organization(organization)
                .build();

        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setEmail("admin@westminster.org");
        adminLogin.setPassword("Password123!");

        when(userRepository.findByEmail("admin@westminster.org")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("Password123!", "encodedAdminPass")).thenReturn(true);
        when(jwtUtil.generateToken("admin@westminster.org")).thenReturn("adminOrgJwtToken");
        when(organizationAdminRepository.findByUserId(10L)).thenReturn(Optional.of(orgAdmin));

        AuthResponse response = authService.login(adminLogin);

        assertNotNull(response);
        assertEquals("admin@westminster.org", response.getEmail());
        assertEquals("ADMIN", response.getRole());
        assertEquals("adminOrgJwtToken", response.getToken());
        assertEquals("Westminster Community Church", response.getOrganizationName());
        assertEquals("ORG-000002", response.getOrganizationCode());
        assertEquals("SW1A 1AA", response.getPostcode());
        assertEquals("APPROVED", response.getApprovalStatus());
        assertNotNull(response.getUser());
        assertEquals(10L, response.getUser().getId());
        assertEquals("Westminster Admin", response.getUser().getFullName());
    }

    @Test
    public void login_SuperAdmin_DoesNotIncludeOrganizationDetails() {
        User superAdmin = User.builder()
                .id(99L)
                .fullName("Super Admin")
                .email("superadmin@rememberme.com")
                .password("encodedSuperPass")
                .role(User.Role.SUPER_ADMIN)
                .build();

        LoginRequest superAdminLogin = new LoginRequest();
        superAdminLogin.setEmail("superadmin@rememberme.com");
        superAdminLogin.setPassword("SuperAdmin@123");

        when(userRepository.findByEmail("superadmin@rememberme.com")).thenReturn(Optional.of(superAdmin));
        when(passwordEncoder.matches("SuperAdmin@123", "encodedSuperPass")).thenReturn(true);
        when(jwtUtil.generateToken("superadmin@rememberme.com")).thenReturn("superAdminJwtToken");

        AuthResponse response = authService.login(superAdminLogin);

        assertNotNull(response);
        assertEquals("superadmin@rememberme.com", response.getEmail());
        assertEquals("SUPER_ADMIN", response.getRole());
        assertEquals("superAdminJwtToken", response.getToken());
        assertNull(response.getOrganizationName());
        assertNull(response.getOrganizationCode());
        assertNull(response.getPostcode());
        assertNull(response.getApprovalStatus());
        verifyNoInteractions(organizationAdminRepository);
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
