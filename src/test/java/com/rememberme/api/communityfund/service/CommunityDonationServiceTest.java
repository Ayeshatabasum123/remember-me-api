package com.rememberme.api.communityfund.service;

import com.rememberme.api.communityfund.config.CommunityFundProperties;
import com.rememberme.api.communityfund.dto.request.DonationCheckoutRequest;
import com.rememberme.api.communityfund.dto.response.*;
import com.rememberme.api.communityfund.entity.PaymentTransaction;
import com.rememberme.api.communityfund.exception.CommunityFundAccessDeniedException;
import com.rememberme.api.communityfund.exception.CommunityFundValidationException;
import com.rememberme.api.communityfund.mapper.FundMapper;
import com.rememberme.api.communityfund.repository.PaymentTransactionRepository;
import com.rememberme.api.communityfund.security.CommunityFundSecurityHelper;
import com.rememberme.api.communityfund.service.impl.CommunityDonationServiceImpl;
import com.rememberme.api.entity.*;
import com.rememberme.api.exception.InvalidPaginationException;
import com.rememberme.api.repository.DonationReceiptRepository;
import com.rememberme.api.repository.DonationRepository;
import com.rememberme.api.service.StripePaymentService;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommunityDonationServiceTest {

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private DonationReceiptRepository donationReceiptRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private StripePaymentService stripePaymentService;

    @Mock
    private CommunityFundSecurityHelper securityHelper;

    private CommunityFundProperties properties;
    private FundMapper fundMapper;
    private CommunityDonationServiceImpl donationService;

    private User donor;
    private User otherUser;

    @BeforeEach
    void setUp() {
        properties = new CommunityFundProperties();
        fundMapper = new FundMapper();
        donationService = new CommunityDonationServiceImpl(
                donationRepository,
                donationReceiptRepository,
                paymentTransactionRepository,
                stripePaymentService,
                properties,
                fundMapper,
                securityHelper
        );

        donor = User.builder()
                .id(1L)
                .email("donor@example.com")
                .fullName("John Smith")
                .role(User.Role.USER)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .fullName("Other User")
                .role(User.Role.USER)
                .build();
    }

    @Test
    @DisplayName("getDonationAmounts returns presets £5, £10, £50 and custom range")
    void testGetDonationAmounts() {
        DonationAmountsResponse response = donationService.getDonationAmounts();

        assertThat(response).isNotNull();
        assertThat(response.getCurrency()).isEqualTo("GBP");
        assertThat(response.getCurrencySymbol()).isEqualTo("£");
        assertThat(response.getPresets()).hasSize(3);
        assertThat(response.getCustomAmount().getMinAmount()).isEqualByComparingTo(BigDecimal.valueOf(1.00));
        assertThat(response.getCustomAmount().getMaxAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000.00));
    }

    @Test
    @DisplayName("createCheckout succeeds for valid amount and loved-one dedication")
    void testCreateCheckout_Success() throws Exception {
        DonationCheckoutRequest request = DonationCheckoutRequest.builder()
                .amount(BigDecimal.valueOf(50.00))
                .currency("GBP")
                .dedication(DonationCheckoutRequest.DedicationRequest.builder()
                        .enabled(true)
                        .lovedOneNames(List.of("Grandmother Sarah"))
                        .message("In loving memory")
                        .build())
                .build();

        PaymentIntent mockPi = mock(PaymentIntent.class);
        when(mockPi.getId()).thenReturn("pi_test_123");
        when(mockPi.getClientSecret()).thenReturn("pi_test_123_secret_abc");
        when(mockPi.getCustomer()).thenReturn("cus_test_123");

        when(stripePaymentService.createPaymentIntent(eq(5000L), eq("GBP"), any(), eq("donor@example.com"), eq("idemp_1")))
                .thenReturn(mockPi);

        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> {
            Donation d = invocation.getArgument(0);
            if (d.getId() == null) {
                d.setId(101L);
            }
            return d;
        });

        DonationCheckoutResponse response = donationService.createCheckout(donor, request, "idemp_1");

        assertThat(response).isNotNull();
        assertThat(response.getDonationId()).isEqualTo(101L);
        assertThat(response.getClientSecret()).isEqualTo("pi_test_123_secret_abc");
        assertThat(response.getStripePaymentIntentId()).isEqualTo("pi_test_123");
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        assertThat(response.getCurrency()).isEqualTo("GBP");
        assertThat(response.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(response.getDedicationNames()).contains("Grandmother Sarah");
        assertThat(response.getDedicationMessage()).isEqualTo("In loving memory");

        verify(paymentTransactionRepository, times(1)).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("createCheckout rejects negative or below-minimum donation amount")
    void testCreateCheckout_InvalidAmount_ThrowsValidationException() {
        DonationCheckoutRequest request = DonationCheckoutRequest.builder()
                .amount(BigDecimal.valueOf(0.50))
                .currency("GBP")
                .build();

        assertThatThrownBy(() -> donationService.createCheckout(donor, request, null))
                .isInstanceOf(CommunityFundValidationException.class)
                .hasMessageContaining("at least £1.00");
    }

    @Test
    @DisplayName("createCheckout rejects unsupported non-GBP currency")
    void testCreateCheckout_InvalidCurrency_ThrowsValidationException() {
        DonationCheckoutRequest request = DonationCheckoutRequest.builder()
                .amount(BigDecimal.valueOf(50.00))
                .currency("USD")
                .build();

        assertThatThrownBy(() -> donationService.createCheckout(donor, request, null))
                .isInstanceOf(CommunityFundValidationException.class)
                .hasMessageContaining("Only GBP currency is currently supported");
    }

    @Test
    @DisplayName("createCheckout returns existing donation idempotently on duplicate key")
    void testCreateCheckout_Idempotency() {
        Donation existing = Donation.builder()
                .id(202L)
                .user(donor)
                .amountPence(5000L)
                .currency("GBP")
                .paymentStatus(DonationPaymentStatus.PENDING)
                .stripePaymentIntentId("pi_existing")
                .clientSecret("pi_existing_secret")
                .idempotencyKey("idemp_dup")
                .build();

        when(donationRepository.findByIdempotencyKey("idemp_dup")).thenReturn(Optional.of(existing));

        DonationCheckoutRequest request = DonationCheckoutRequest.builder()
                .amount(BigDecimal.valueOf(50.00))
                .currency("GBP")
                .build();

        DonationCheckoutResponse response = donationService.createCheckout(donor, request, "idemp_dup");

        assertThat(response.getDonationId()).isEqualTo(202L);
        assertThat(response.getClientSecret()).isEqualTo("pi_existing_secret");
        verifyNoInteractions(stripePaymentService);
    }

    @Test
    @DisplayName("getDonationDetails returns details for authorized owner")
    void testGetDonationDetails_Authorized() {
        Donation d = Donation.builder()
                .id(101L)
                .user(donor)
                .amountPence(2500L)
                .currency("GBP")
                .paymentStatus(DonationPaymentStatus.PENDING)
                .stripePaymentIntentId("pi_101")
                .clientSecret("pi_101_secret")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(donationRepository.findById(101L)).thenReturn(Optional.of(d));
        when(donationReceiptRepository.findByDonationId(101L)).thenReturn(Optional.empty());

        DonationDetailsResponse response = donationService.getDonationDetails(101L, donor);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(25.00));
        assertThat(response.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(response.isReceiptAvailable()).isFalse();
    }

    @Test
    @DisplayName("getDonationReceipt throws validation exception if payment is not yet confirmed")
    void testGetDonationReceipt_NotConfirmed_ThrowsException() {
        Donation d = Donation.builder()
                .id(101L)
                .user(donor)
                .amountPence(2500L)
                .paymentStatus(DonationPaymentStatus.PENDING)
                .build();

        when(donationRepository.findById(101L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> donationService.getDonationReceipt(101L, donor))
                .isInstanceOf(CommunityFundValidationException.class)
                .hasMessageContaining("confirmed, successful donations");
    }

    @Test
    @DisplayName("getDonationReceipt returns receipt details for confirmed payment")
    void testGetDonationReceipt_Success() {
        Donation d = Donation.builder()
                .id(101L)
                .user(donor)
                .amountPence(2500L)
                .currency("GBP")
                .paymentStatus(DonationPaymentStatus.SUCCEEDED)
                .build();

        DonationReceipt receipt = DonationReceipt.builder()
                .id(5L)
                .donation(d)
                .receiptNumber("RCP-20261021-TEST99")
                .donorName("John Smith")
                .donorEmail("donor@example.com")
                .amountPence(2500L)
                .currency("GBP")
                .paymentMethod("card")
                .fundName("Remember Me Community Burial Fund")
                .issuedAt(LocalDateTime.now())
                .build();

        when(donationRepository.findById(101L)).thenReturn(Optional.of(d));
        when(donationReceiptRepository.findByDonationId(101L)).thenReturn(Optional.of(receipt));

        DonationReceiptInfoResponse response = donationService.getDonationReceipt(101L, donor);

        assertThat(response).isNotNull();
        assertThat(response.getReceiptNumber()).isEqualTo("RCP-20261021-TEST99");
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(25.00));
        assertThat(response.getDonorName()).isEqualTo("John Smith");
    }

    @Test
    @DisplayName("retryDonation rejects retrying an already completed donation")
    void testRetryDonation_AlreadyConfirmed_ThrowsException() {
        Donation d = Donation.builder()
                .id(101L)
                .user(donor)
                .amountPence(5000L)
                .paymentStatus(DonationPaymentStatus.SUCCEEDED)
                .build();

        when(donationRepository.findById(101L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> donationService.retryDonation(101L, donor))
                .isInstanceOf(CommunityFundValidationException.class)
                .hasMessageContaining("already been successfully confirmed");
    }

    @Test
    @DisplayName("retryDonation refreshes payment session safely for failed donation")
    void testRetryDonation_Success() throws Exception {
        Donation d = Donation.builder()
                .id(101L)
                .user(donor)
                .amountPence(5000L)
                .currency("GBP")
                .donorEmail("donor@example.com")
                .paymentStatus(DonationPaymentStatus.FAILED)
                .stripePaymentIntentId("pi_failed")
                .build();

        PaymentIntent newPi = mock(PaymentIntent.class);
        when(newPi.getId()).thenReturn("pi_fresh_456");
        when(newPi.getClientSecret()).thenReturn("pi_fresh_456_secret");

        when(donationRepository.findById(101L)).thenReturn(Optional.of(d));
        when(stripePaymentService.retrievePaymentIntent("pi_failed")).thenReturn(null);
        when(stripePaymentService.createPaymentIntent(5000L, "GBP", 101L, "donor@example.com", null))
                .thenReturn(newPi);

        DonationRetryResponse response = donationService.retryDonation(101L, donor);

        assertThat(response).isNotNull();
        assertThat(response.getDonationId()).isEqualTo(101L);
        assertThat(response.getClientSecret()).isEqualTo("pi_fresh_456_secret");
        assertThat(response.getStripePaymentIntentId()).isEqualTo("pi_fresh_456");
        assertThat(response.getStatus()).isEqualTo("PENDING");

        verify(paymentTransactionRepository, times(1)).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("getMyDonations returns paginated donations for authenticated user")
    void testGetMyDonations_Success() {
        Donation d1 = Donation.builder()
                .id(101L)
                .user(donor)
                .amountPence(5000L)
                .currency("GBP")
                .paymentStatus(DonationPaymentStatus.SUCCEEDED)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        Donation d2 = Donation.builder()
                .id(102L)
                .user(donor)
                .amountPence(2500L)
                .currency("GBP")
                .paymentStatus(DonationPaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Donation> page = new PageImpl<>(List.of(d2, d1), PageRequest.of(0, 10), 2);
        when(donationRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);
        when(donationReceiptRepository.findByDonationId(anyLong())).thenReturn(Optional.empty());

        Page<DonationDetailsResponse> result = donationService.getMyDonations(donor, 0, 10, "createdAt", "DESC");

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(102L);
        assertThat(result.getContent().get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(25.00));
        assertThat(result.getContent().get(1).getId()).isEqualTo(101L);
        assertThat(result.getContent().get(1).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
    }

    @Test
    @DisplayName("getMyDonations returns empty page when user has no donations")
    void testGetMyDonations_Empty() {
        when(donationRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(Page.empty());

        Page<DonationDetailsResponse> result = donationService.getMyDonations(donor, 0, 10, "createdAt", "DESC");

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("getMyDonations throws InvalidPaginationException on negative page")
    void testGetMyDonations_NegativePage_ThrowsException() {
        assertThatThrownBy(() -> donationService.getMyDonations(donor, -1, 10, "createdAt", "DESC"))
                .isInstanceOf(InvalidPaginationException.class)
                .hasMessageContaining("Page index must be non-negative");
    }

    @Test
    @DisplayName("getMyDonations throws InvalidPaginationException on size > 100")
    void testGetMyDonations_ExcessiveSize_ThrowsException() {
        assertThatThrownBy(() -> donationService.getMyDonations(donor, 0, 101, "createdAt", "DESC"))
                .isInstanceOf(InvalidPaginationException.class)
                .hasMessageContaining("size must be between 1 and 100");
    }

    @Test
    @DisplayName("getMyDonations throws CommunityFundAccessDeniedException on null user")
    void testGetMyDonations_NullUser_ThrowsException() {
        assertThatThrownBy(() -> donationService.getMyDonations(null, 0, 10, "createdAt", "DESC"))
                .isInstanceOf(CommunityFundAccessDeniedException.class)
                .hasMessageContaining("Authenticated user context is required");
    }

    @Test
    @DisplayName("getDonationDetails rejects unauthorized user accessing another donor's record")
    void testGetDonationDetails_Unauthorized_ThrowsAccessDenied() {
        Donation d = Donation.builder()
                .id(101L)
                .user(donor)
                .amountPence(2500L)
                .build();

        when(donationRepository.findById(101L)).thenReturn(Optional.of(d));
        doThrow(new CommunityFundAccessDeniedException("You do not have permission"))
                .when(securityHelper).verifyOwnerOrStaff(donor.getId());

        assertThatThrownBy(() -> donationService.getDonationDetails(101L, otherUser))
                .isInstanceOf(CommunityFundAccessDeniedException.class);
    }

    @Test
    @DisplayName("getDonationReceipt rejects unauthorized user accessing another donor's receipt")
    void testGetDonationReceipt_Unauthorized_ThrowsAccessDenied() {
        Donation d = Donation.builder()
                .id(101L)
                .user(donor)
                .amountPence(2500L)
                .paymentStatus(DonationPaymentStatus.SUCCEEDED)
                .build();

        when(donationRepository.findById(101L)).thenReturn(Optional.of(d));
        doThrow(new CommunityFundAccessDeniedException("You do not have permission"))
                .when(securityHelper).verifyOwnerOrStaff(donor.getId());

        assertThatThrownBy(() -> donationService.getDonationReceipt(101L, otherUser))
                .isInstanceOf(CommunityFundAccessDeniedException.class);
    }
}
