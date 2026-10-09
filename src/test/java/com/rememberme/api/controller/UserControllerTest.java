package com.rememberme.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rememberme.api.config.SecurityConfig;
import com.rememberme.api.entity.User;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtAuthFilter;
import com.rememberme.api.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private User mockAdmin;
    private User mockUser;

    @BeforeEach
    public void setUp() {
        mockAdmin = User.builder()
                .id(1L)
                .fullName("Admin")
                .email("admin@example.com")
                .phone("9876543210")
                .country("India")
                .role(User.Role.ADMIN)
                .build();

        mockUser = User.builder()
                .id(2L)
                .fullName("John Doe")
                .email("user@example.com")
                .phone("1234567890")
                .country("USA")
                .role(User.Role.USER)
                .build();
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    public void getMe_AsAdmin_ReturnsAdminRole() throws Exception {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(mockAdmin));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.fullName").value("Admin"))
                .andExpect(jsonPath("$.data.email").value("admin@example.com"))
                .andExpect(jsonPath("$.data.phone").value("9876543210"))
                .andExpect(jsonPath("$.data.country").value("India"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    public void getMe_AsUser_ReturnsUserRole() throws Exception {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.fullName").value("John Doe"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    public void getMe_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    public void updateUser_DoesNotAllowRoleModification() throws Exception {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Attempting to escalate role to ADMIN via PUT /api/users/2
        String requestJson = "{" +
                "\"fullName\": \"Updated Name\"," +
                "\"phone\": \"9999999999\"," +
                "\"country\": \"Canada\"," +
                "\"role\": \"ADMIN\"" +
                "}";

        mockMvc.perform(put("/api/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.data.role").value("USER")); // Role must remain USER!

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(User.Role.USER, userCaptor.getValue().getRole());
    }
}
