package com.rememberme.api.service;

import com.rememberme.api.config.StripeConfig;
import com.rememberme.api.dto.request.CreateDonationPaymentIntentRequest;
import com.rememberme.api.dto.request.DedicationRequest;
import com.rememberme.api.dto.response.CreatePaymentIntentResponse;
import com.rememberme.api.dto.response.DonationOptionsResponse;
import com.rememberme.api.dto.response.DonationResponse;
import com.rememberme.api.entity.*;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.DeceasedPersonRepository;
import com.rememberme.api.repository.DonationDedicationRepository;
import com.rememberme.api.repository.DonationReceiptRepository;
import com.rememberme.api.repository.DonationRepository;
import com.rememberme.api.repository.MemorialRepository;
import com.rememberme.api.service.impl.DonationServiceImpl;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DonationServiceTest {

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private DonationDedicationRepository donationDedicationRepository;

    @Mock
    private DonationReceiptRepository donationReceiptRepository;

    @Mock
    private DeceasedPersonRepository deceasedPersonRepository;

    @Mock
    private MemorialRepository memorialRepository;

    @Mock
    private StripePaymentService stripePaymentService;

    @Mock
    private StripeConfig stripeConfig;

    @InjectMocks
    private DonationServiceImpl donationService;

    private User donor;
    private User otherUser;

    @BeforeEach
    public void setUp() {
        donor = User.builder()
                .id(1L)
                .email("donor@example.com")
                .fullName("Amina Begum")
                .role(User.Role.USER)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .fullName("Farhan Ali")
                .role(User.Role.USER)
                .build();
    }

    @Test
    public void getDonationOptions_ReturnsPresetsAndLimits() {
        when(stripeConfig.getPublishableKey()).thenReturn("pk_test_12345");

        DonationOptionsResponse options = donationService.getDonationOptions();

        assertNotNull(options);
        assertEquals("GBP", options.getCurrency());
        assertEquals("£", options.getCurrencySymbol());
        assertEquals(3, options.getPresetAmounts().size());
        assertEquals(500L, options.getPresetAmounts().get(0).getAmountPence());
        assertEquals(1000L, options.getPresetAmounts().get(1).getAmountPence());
        assertEquals(5000L, options.getPresetAmounts().get(2).getAmountPence());
        assertEquals(100L, options.getMinCustomAmountPence());
        assertEquals(500000L, options.getMaxCustomAmountPence());
        assertEquals("pk_test_12345", options.getStripePublishableKey());
        assertTrue(options.getSharedFundExplanation().contains("Community Burial Fund"));
    }

    @Test
    public void createPaymentIntent_Success() throws Exception {
        CreateDonationPaymentIntentRequest request = CreateDonationPaymentIntentRequest.builder()
                .amountPence(1000L)
                .currency("GBP")
                .idempotencyKey("idem-test-101")
                .dedications(List.of(
                        DedicationRequest.builder()
                                .dedicationName("Grandfather Tariq")
                                .message("Rest in peaceful mercy.")
                                .build()
                ))
                .build();

        when(donationRepository.findByIdempotencyKey("idem-test-101")).thenReturn(Optional.empty());

        Donation savedDonation = Donation.builder()
                .id(101L)
                .user(donor)
                .amountPence(1000L)
                .currency("GBP")
                .paymentStatus(DonationPaymentStatus.PENDING)
                .settlementStatus(DonationSettlementStatus.PENDING)
                .idempotencyKey("idem-test-101")
                .donorName("Amina Begum")
                .donorEmail("donor@example.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(donationRepository.save(any(Donation.class))).thenReturn(savedDonation);

        PaymentIntent mockPi = mock(PaymentIntent.class);
        when(mockPi.getId()).thenReturn("pi_mock_123");
        when(mockPi.getClientSecret()).thenReturn("pi_mock_123_secret_xyz");

        when(stripePaymentService.createPaymentIntent(eq(1000L), eq("GBP"), eq(101L), eq("donor@example.com"), eq("idem-test-101")))
                .thenReturn(mockPi);

        CreatePaymentIntentResponse response = donationService.createPaymentIntent(donor, request, null);

        assertNotNull(response);
        assertEquals(101L, response.getDonationId());
        assertEquals("pi_mock_123", response.getPaymentIntentId());
        assertEquals("pi_mock_123_secret_xyz", response.getClientSecret());
        assertEquals(1000L, response.getAmountPence());
        assertEquals("GBP", response.getCurrency());
        assertEquals("PENDING", response.getStatus());

        verify(donationDedicationRepository, times(1)).saveAll(anyList());
    }

    @Test
    public void createPaymentIntent_AmountBelowMinimum_ThrowsBadRequest() {
        CreateDonationPaymentIntentRequest request = CreateDonationPaymentIntentRequest.builder()
                .amountPence(50L) // Below 100 pence (£1.00)
                .currency("GBP")
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                donationService.createPaymentIntent(donor, request, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Invalid donation amount"));
    }

    @Test
    public void createPaymentIntent_AmountAboveMaximum_ThrowsBadRequest() {
        CreateDonationPaymentIntentRequest request = CreateDonationPaymentIntentRequest.builder()
                .amountPence(600000L) // Above 500,000 pence (£5,000.00)
                .currency("GBP")
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                donationService.createPaymentIntent(donor, request, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Invalid donation amount"));
    }

    @Test
    public void createPaymentIntent_InvalidCurrency_ThrowsBadRequest() {
        CreateDonationPaymentIntentRequest request = CreateDonationPaymentIntentRequest.builder()
                .amountPence(1000L)
                .currency("USD") // Non-GBP
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                donationService.createPaymentIntent(donor, request, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Only GBP currency is accepted"));
    }

    @Test
    public void createPaymentIntent_IdempotentReuse_ReturnsExistingWithoutCallingStripe() {
        CreateDonationPaymentIntentRequest request = CreateDonationPaymentIntentRequest.builder()
                .amountPence(1000L)
                .currency("GBP")
                .idempotencyKey("idem-duplicate-key")
                .build();

        Donation existingDonation = Donation.builder()
                .id(999L)
                .user(donor)
                .amountPence(1000L)
                .currency("GBP")
                .stripePaymentIntentId("pi_existing_999")
                .clientSecret("pi_existing_999_secret")
                .paymentStatus(DonationPaymentStatus.PENDING)
                .idempotencyKey("idem-duplicate-key")
                .build();

        when(donationRepository.findByIdempotencyKey("idem-duplicate-key")).thenReturn(Optional.of(existingDonation));

        CreatePaymentIntentResponse response = donationService.createPaymentIntent(donor, request, null);

        assertNotNull(response);
        assertEquals(999L, response.getDonationId());
        assertEquals("pi_existing_999", response.getPaymentIntentId());
        assertEquals("pi_existing_999_secret", response.getClientSecret());

        // Verify Stripe was NOT called again
        verifyNoInteractions(stripePaymentService);
    }

    @Test
    public void createPaymentIntent_IdempotencyKeyBelongsToDifferentUser_ThrowsConflict() {
        CreateDonationPaymentIntentRequest request = CreateDonationPaymentIntentRequest.builder()
                .amountPence(1000L)
                .currency("GBP")
                .idempotencyKey("shared-key")
                .build();

        Donation existingDonation = Donation.builder()
                .id(888L)
                .user(otherUser) // Owned by other user
                .amountPence(1000L)
                .idempotencyKey("shared-key")
                .build();

        when(donationRepository.findByIdempotencyKey("shared-key")).thenReturn(Optional.of(existingDonation));

        ApiException ex = assertThrows(ApiException.class, () ->
                donationService.createPaymentIntent(donor, request, null));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    public void createPaymentIntent_InvalidDeceasedPersonTarget_ThrowsBadRequest() {
        CreateDonationPaymentIntentRequest request = CreateDonationPaymentIntentRequest.builder()
                .amountPence(1000L)
                .currency("GBP")
                .dedications(List.of(
                        DedicationRequest.builder()
                                .dedicationName("Grandfather")
                                .deceasedPersonId(99999L) // Non-existent deceased person
                                .build()
                ))
                .build();

        when(deceasedPersonRepository.existsById(99999L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () ->
                donationService.createPaymentIntent(donor, request, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("deceased person ID 99999 does not exist"));
    }

    @Test
    public void getMyDonationById_OwnershipEnforced_ThrowsForbiddenForDifferentUser() {
        Donation donation = Donation.builder()
                .id(101L)
                .user(otherUser) // Owned by otherUser (id 2)
                .amountPence(1000L)
                .build();

        when(donationRepository.findById(101L)).thenReturn(Optional.of(donation));

        // Donor with id 1 requests it
        ApiException ex = assertThrows(ApiException.class, () ->
                donationService.getMyDonationById(101L, donor.getId()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("You can only view your own donations"));
    }

    @Test
    public void getMyDonationById_Success() {
        Donation donation = Donation.builder()
                .id(101L)
                .user(donor)
                .amountPence(1000L)
                .currency("GBP")
                .paymentStatus(DonationPaymentStatus.SUCCEEDED)
                .settlementStatus(DonationSettlementStatus.SETTLED)
                .donorName("Amina Begum")
                .donorEmail("donor@example.com")
                .createdAt(LocalDateTime.now())
                .build();

        when(donationRepository.findById(101L)).thenReturn(Optional.of(donation));
        when(donationDedicationRepository.findByDonationId(101L)).thenReturn(Collections.emptyList());
        when(donationReceiptRepository.findByDonationId(101L)).thenReturn(Optional.empty());

        DonationResponse res = donationService.getMyDonationById(101L, donor.getId());

        assertNotNull(res);
        assertEquals(101L, res.getId());
        assertEquals(1000L, res.getAmountPence());
        assertEquals("£10.00", res.getAmountFormatted());
        assertEquals("SUCCEEDED", res.getPaymentStatus());
        assertEquals("SETTLED", res.getSettlementStatus());
    }

    @Test
    public void getMyDonations_Paginated() {
        Donation donation = Donation.builder()
                .id(101L)
                .user(donor)
                .amountPence(1000L)
                .currency("GBP")
                .paymentStatus(DonationPaymentStatus.SUCCEEDED)
                .settlementStatus(DonationSettlementStatus.SETTLED)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Donation> page = new PageImpl<>(List.of(donation));
        when(donationRepository.findByUser(eq(donor), any())).thenReturn(page);
        when(donationDedicationRepository.findByDonationId(101L)).thenReturn(Collections.emptyList());
        when(donationReceiptRepository.findByDonationId(101L)).thenReturn(Optional.empty());

        Page<DonationResponse> res = donationService.getMyDonations(donor, PageRequest.of(0, 10));

        assertNotNull(res);
        assertEquals(1, res.getTotalElements());
        assertEquals(101L, res.getContent().get(0).getId());
    }
}
