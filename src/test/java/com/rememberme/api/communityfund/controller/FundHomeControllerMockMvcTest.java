package com.rememberme.api.communityfund.controller;

import com.rememberme.api.communityfund.dto.response.FundHomeResponse;
import com.rememberme.api.communityfund.dto.response.FundImpactResponse;
import com.rememberme.api.communityfund.dto.response.FundPublicBalanceSummaryResponse;
import com.rememberme.api.communityfund.service.FundHomeService;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FundHomeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class FundHomeControllerMockMvcTest {

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
    @DisplayName("GET /api/fund/home returns 200 OK with complete fund information")
    void testGetFundHome() throws Exception {
        FundHomeResponse response = FundHomeResponse.builder()
                .fundName("Remember Me Community Burial Fund")
                .introduction("Providing compassionate, communal burial assistance.")
                .purpose("To relieve funeral poverty.")
                .description("A community-wide safety net.")
                .donationInformation("Contributions of any amount accepted.")
                .privacyInformation("Applicant details are kept strictly confidential.")
                .fundingDisclaimer("Assistance is subject to fund availability.")
                .updatedAt(LocalDateTime.now())
                .build();

        when(fundHomeService.getFundHomeInfo()).thenReturn(response);

        mockMvc.perform(get("/api/fund/home")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Fund home information retrieved successfully"))
                .andExpect(jsonPath("$.data.fundName").value("Remember Me Community Burial Fund"))
                .andExpect(jsonPath("$.data.purpose").value("To relieve funeral poverty."))
                .andExpect(jsonPath("$.data.privacyInformation").value("Applicant details are kept strictly confidential."))
                .andExpect(jsonPath("$.data.fundingDisclaimer").value("Assistance is subject to fund availability."));
    }

    @Test
    @DisplayName("GET /api/fund/impact returns 200 OK with confirmed aggregated impact statistics")
    void testGetFundImpact() throws Exception {
        FundImpactResponse response = FundImpactResponse.builder()
                .currency("GBP")
                .currencySymbol("£")
                .totalConfirmedDonations(125)
                .totalAmountReceived(BigDecimal.valueOf(6250.00))
                .totalAmountReceivedFormatted("£6,250.00")
                .totalCompletedSupportCases(8)
                .totalApprovedSupportDisbursed(BigDecimal.valueOf(4500.00))
                .totalApprovedSupportDisbursedFormatted("£4,500.00")
                .asOf(LocalDateTime.now())
                .build();

        when(fundHomeService.getFundImpact()).thenReturn(response);

        mockMvc.perform(get("/api/fund/impact")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Fund impact statistics retrieved successfully"))
                .andExpect(jsonPath("$.data.currency").value("GBP"))
                .andExpect(jsonPath("$.data.currencySymbol").value("£"))
                .andExpect(jsonPath("$.data.totalConfirmedDonations").value(125))
                .andExpect(jsonPath("$.data.totalCompletedSupportCases").value(8))
                .andExpect(jsonPath("$.data.totalAmountReceivedFormatted").value("£6,250.00"))
                .andExpect(jsonPath("$.data.totalApprovedSupportDisbursedFormatted").value("£4,500.00"));
    }

    @Test
    @DisplayName("GET /api/fund/balance-summary returns 200 OK with approved public balance figures")
    void testGetFundBalanceSummary() throws Exception {
        FundPublicBalanceSummaryResponse response = FundPublicBalanceSummaryResponse.builder()
                .currency("GBP")
                .currencySymbol("£")
                .totalConfirmedDonations(125)
                .totalAmountReceived(BigDecimal.valueOf(6250.00))
                .totalAmountReceivedFormatted("£6,250.00")
                .totalApprovedSupportDisbursed(BigDecimal.valueOf(4500.00))
                .totalApprovedSupportDisbursedFormatted("£4,500.00")
                .availableBalance(BigDecimal.valueOf(1750.00))
                .availableBalanceFormatted("£1,750.00")
                .asOf(LocalDateTime.now())
                .build();

        when(fundHomeService.getPublicBalanceSummary()).thenReturn(response);

        mockMvc.perform(get("/api/fund/balance-summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Fund balance summary retrieved successfully"))
                .andExpect(jsonPath("$.data.currency").value("GBP"))
                .andExpect(jsonPath("$.data.totalConfirmedDonations").value(125))
                .andExpect(jsonPath("$.data.availableBalanceFormatted").value("£1,750.00"));
    }
}
