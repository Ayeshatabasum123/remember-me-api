package com.rememberme.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rememberme.api.controller.AdminController;
import com.rememberme.api.controller.AuthController;
import com.rememberme.api.dto.request.LoginRequest;
import com.rememberme.api.dto.response.AuthResponse;
import com.rememberme.api.repository.RememberMeRepository;
import com.rememberme.api.repository.ReportRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtAuthFilter;
import com.rememberme.api.security.JwtUtil;
import com.rememberme.api.service.AuthService;
import com.rememberme.api.service.GraveImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, AdminController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class})
public class CorsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private GraveImportService graveImportService;

    @MockBean
    private RememberMeRepository rememberMeRepository;

    @MockBean
    private ReportRepository reportRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void options_PreflightLogin_AllowedFlutterOrigin_ReturnsOkWithCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:65172")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type, Accept"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:65172"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
    }

    @Test
    public void options_PreflightDynamicPort_LocalhostAnd127_Allowed() throws Exception {
        // Dynamic localhost port
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:54321")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:54321"));

        // 127.0.0.1 development port
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:43210")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:43210"));
    }

    @Test
    public void options_PreflightAdminEndpoint_WithoutAuth_Allowed() throws Exception {
        mockMvc.perform(options("/api/admin/graves/import")
                        .header(HttpHeaders.ORIGIN, "http://localhost:65172")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:65172"));
    }

    @Test
    public void options_PreflightDisallowedPublicOrigin_NotAllowed() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://untrusted-public-origin.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void post_LoginRequest_WithCorsOrigin_ReturnsCorsHeaders() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin-test@example.com");
        request.setPassword("Admin@123");

        AuthResponse authResponse = AuthResponse.builder()
                .token("mockJwtToken")
                .email("admin-test@example.com")
                .role("ADMIN")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:65172")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:65172"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }
}
