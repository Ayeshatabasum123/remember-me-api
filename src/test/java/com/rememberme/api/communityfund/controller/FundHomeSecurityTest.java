package com.rememberme.api.communityfund.controller;

import com.rememberme.api.communityfund.dto.response.FundHomeResponse;
import com.rememberme.api.communityfund.dto.response.FundImpactResponse;
import com.rememberme.api.communityfund.dto.response.FundPublicBalanceSummaryResponse;
import com.rememberme.api.communityfund.service.FundHomeService;
import com.rememberme.api.config.SecurityConfig;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtAuthFilter;
import com.rememberme.api.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FundHomeController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
public class FundHomeSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FundHomeService fundHomeService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Security Check: GET /api/fund/home is publicly accessible without JWT token")
    void testPublicAccess_FundHome_NoAuthRequired() throws Exception {
        when(fundHomeService.getFundHomeInfo()).thenReturn(
                FundHomeResponse.builder().fundName("Remember Me Community Burial Fund").build()
        );

        mockMvc.perform(get("/api/fund/home")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Security Check: GET /api/fund/impact is publicly accessible without JWT token")
    void testPublicAccess_FundImpact_NoAuthRequired() throws Exception {
        when(fundHomeService.getFundImpact()).thenReturn(
                FundImpactResponse.builder()
                        .currency("GBP")
                        .totalAmountReceived(BigDecimal.valueOf(100.00))
                        .build()
        );

        mockMvc.perform(get("/api/fund/impact")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Security Check: GET /api/fund/balance-summary is publicly accessible without JWT token")
    void testPublicAccess_FundBalanceSummary_NoAuthRequired() throws Exception {
        when(fundHomeService.getPublicBalanceSummary()).thenReturn(
                FundPublicBalanceSummaryResponse.builder()
                        .currency("GBP")
                        .availableBalance(BigDecimal.valueOf(500.00))
                        .build()
        );

        mockMvc.perform(get("/api/fund/balance-summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
