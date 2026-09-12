package com.rememberme.api.controller;

import com.rememberme.api.config.SecurityConfig;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.entity.Report;
import com.rememberme.api.entity.User;
import com.rememberme.api.repository.GraveRepository;
import com.rememberme.api.repository.RememberMeRepository;
import com.rememberme.api.repository.ReportRepository;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtAuthFilter;
import com.rememberme.api.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminController.class, RememberMeController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class})
public class AdminSecurityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RememberMeRepository rememberMeRepository;

    @MockBean
    private ReportRepository reportRepository;

    @MockBean
    private GraveRepository graveRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private RememberMe mockRememberMe;
    private Report mockReport;

    @BeforeEach
    public void setUp() {
        mockRememberMe = RememberMe.builder()
                .id(1L)
                .name("Test Graveyard")
                .status(RememberMe.ApprovalStatus.PENDING)
                .build();

        mockReport = Report.builder()
                .id(10L)
                .status(Report.ReportStatus.OPEN)
                .build();

        when(rememberMeRepository.findById(1L)).thenReturn(Optional.of(mockRememberMe));
        when(rememberMeRepository.save(any(RememberMe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(reportRepository.findById(10L)).thenReturn(Optional.of(mockReport));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- NORMAL USER ACCESS (Expecting 403 Forbidden) ---

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    public void approveRememberMe_ForbiddenForUser() throws Exception {
        mockMvc.perform(put("/api/admin/rememberMes/1/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    public void rejectRememberMe_ForbiddenForUser() throws Exception {
        mockMvc.perform(put("/api/admin/rememberMes/1/reject"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    public void resolveReport_ForbiddenForUser() throws Exception {
        mockMvc.perform(put("/api/admin/reports/10/resolve"))
                .andExpect(status().isForbidden());
    }

    // --- ADMIN ACCESS (Expecting 200 OK) ---

    @Test
    @WithMockUser(username = "admin-test@example.com", roles = {"ADMIN"})
    public void approveRememberMe_SuccessForAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/rememberMes/1/approve"))
                .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("RememberMe approved"));
    }

    @Test
    @WithMockUser(username = "admin-test@example.com", roles = {"ADMIN"})
    public void rejectRememberMe_SuccessForAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/rememberMes/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("RememberMe rejected"));
    }

    @Test
    @WithMockUser(username = "admin-test@example.com", roles = {"ADMIN"})
    public void resolveReport_SuccessForAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/reports/10/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Report resolved"));
    }

    // --- GET /api/graveyards Endpoint verification ---

    @Test
    @WithMockUser(username = "admin-test@example.com", roles = {"ADMIN"})
    public void getGraveyards_SuccessForAdmin() throws Exception {
        when(rememberMeRepository.findAll()).thenReturn(List.of(mockRememberMe));

        mockMvc.perform(get("/api/graveyards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
