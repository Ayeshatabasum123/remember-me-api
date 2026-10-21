package com.rememberme.api.communityfund.controller;

import com.rememberme.api.communityfund.dto.response.DonationAmountsResponse;
import com.rememberme.api.communityfund.security.CommunityFundSecurityHelper;
import com.rememberme.api.communityfund.service.CommunityDonationService;
import com.rememberme.api.config.SecurityConfig;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtAuthFilter;
import com.rememberme.api.security.JwtUtil;
import com.rememberme.api.service.StripeWebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CommunityDonationController.class, PaymentWebhookController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class})
public class CommunityDonationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommunityDonationService donationService;

    @MockBean
    private StripeWebhookService stripeWebhookService;

    @MockBean
    private CommunityFundSecurityHelper securityHelper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Security: GET /api/donations/amounts is publicly accessible without token")
    void testPublicAmounts_NoAuth() throws Exception {
        when(donationService.getDonationAmounts()).thenReturn(
                DonationAmountsResponse.builder().currency("GBP").build()
        );

        mockMvc.perform(get("/api/donations/amounts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: POST /api/payments/webhook is publicly accessible for provider callbacks")
    void testPublicWebhook_NoAuth() throws Exception {
        doNothing().when(stripeWebhookService).processWebhook(anyString(), any());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: POST /api/donations/checkout rejects unauthenticated request with 401")
    void testUnauthenticatedCheckout_Returns401() throws Exception {
        mockMvc.perform(post("/api/donations/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 50.00}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: GET /api/donations/{id} rejects unauthenticated request with 401")
    void testUnauthenticatedGetById_Returns401() throws Exception {
        mockMvc.perform(get("/api/donations/101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: GET /api/donations/{id}/receipt rejects unauthenticated request with 401")
    void testUnauthenticatedGetReceipt_Returns401() throws Exception {
        mockMvc.perform(get("/api/donations/101/receipt")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: POST /api/donations/{id}/retry rejects unauthenticated request with 401")
    void testUnauthenticatedRetry_Returns401() throws Exception {
        mockMvc.perform(post("/api/donations/101/retry")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: GET /api/donations/me rejects unauthenticated request with 401")
    void testUnauthenticatedGetMyDonations_Returns401() throws Exception {
        mockMvc.perform(get("/api/donations/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
