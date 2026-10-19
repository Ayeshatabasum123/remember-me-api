package com.rememberme.api.service;

import com.rememberme.api.config.StripeConfig;
import com.rememberme.api.dto.response.DonationReceiptResponse;
import com.rememberme.api.dto.response.FundSummaryResponse;
import com.rememberme.api.entity.Donation;
import com.rememberme.api.entity.DonationPaymentStatus;
import com.rememberme.api.entity.DonationSettlementStatus;
import com.rememberme.api.entity.PaymentEvent;
import com.rememberme.api.entity.User;
import com.rememberme.api.entity.DonationReceipt;
import com.rememberme.api.entity.FundLedger;
import com.rememberme.api.entity.FundLedgerEntryType;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.DonationReceiptRepository;
import com.rememberme.api.repository.DonationRepository;
import com.rememberme.api.repository.FundLedgerRepository;
import com.rememberme.api.repository.PaymentEventRepository;
import com.rememberme.api.service.impl.DonationReceiptServiceImpl;
import com.rememberme.api.service.impl.FundLedgerServiceImpl;
import com.rememberme.api.service.impl.StripeWebhookServiceImpl;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.BalanceTransaction;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StripeWebhookAndLedgerTest {

    @Mock
    private StripeConfig stripeConfig;

    @Mock
    private StripePaymentService stripePaymentService;

    @Mock
    private PaymentEventRepository paymentEventRepository;

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private DonationReceiptRepository donationReceiptRepository;

    @Mock
    private FundLedgerRepository fundLedgerRepository;

    @Mock
    private DonationReceiptService donationReceiptService;

    @Mock
    private FundLedgerService fundLedgerService;

    @InjectMocks
    private StripeWebhookServiceImpl stripeWebhookService;

    private User donor;
    private User otherUser;
    private Donation donation;

    @BeforeEach
    public void setUp() {
        donor = User.builder()
                .id(1L)
                .email("donor@example.com")
                .fullName("Amina Begum")
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("intruder@example.com")
                .fullName("Intruder")
                .build();

        donation = Donation.builder()
                .id(101L)
                .user(donor)
                .amountPence(1000L)
                .currency("GBP")
                .paymentStatus(DonationPaymentStatus.PENDING)
                .settlementStatus(DonationSettlementStatus.PENDING)
                .stripePaymentIntentId("pi_test_101")
                .donorName("Amina Begum")
                .donorEmail("donor@example.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    public void processWebhook_InvalidSignature_ThrowsBadRequest() throws Exception {
        when(stripeConfig.getWebhookSecret()).thenReturn("whsec_secret");
        when(stripePaymentService.constructWebhookEvent(anyString(), anyString(), anyString()))
                .thenThrow(new SignatureVerificationException("Invalid signature", "sig"));

        ApiException ex = assertThrows(ApiException.class, () ->
                stripeWebhookService.processWebhook("payload", "bad_signature"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Invalid Stripe signature"));
    }

    @Test
    public void processWebhook_DuplicateEvent_IdempotentNoOps() throws Exception {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("evt_duplicate_123");
        when(stripePaymentService.constructWebhookEvent(anyString(), any(), any()))
                .thenReturn(mockEvent);

        // Already exists in payment_events table
        when(paymentEventRepository.existsByStripeEventId("evt_duplicate_123")).thenReturn(true);

        stripeWebhookService.processWebhook("payload", "sig");

        // Verify no updates performed on donation, ledger, or receipts
        verifyNoInteractions(donationRepository);
        verifyNoInteractions(donationReceiptService);
        verifyNoInteractions(fundLedgerService);
    }

    @Test
    public void processWebhook_PaymentIntentSucceeded_SuccessSettlementAndReceipt() throws Exception {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("evt_succeeded_101");
        when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

        PaymentIntent mockPi = mock(PaymentIntent.class);
        when(mockPi.getId()).thenReturn("pi_test_101");
        when(mockPi.getLatestCharge()).thenReturn("ch_test_101");

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(mockPi));
        when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);

        when(stripePaymentService.constructWebhookEvent(anyString(), any(), any()))
                .thenReturn(mockEvent);
        when(paymentEventRepository.existsByStripeEventId("evt_succeeded_101")).thenReturn(false);
        when(donationRepository.findByStripePaymentIntentId("pi_test_101")).thenReturn(Optional.of(donation));

        // Mock Charge and BalanceTransaction
        Charge mockCharge = mock(Charge.class);
        when(mockCharge.getBalanceTransaction()).thenReturn("txn_bal_101");
        when(mockCharge.getReceiptUrl()).thenReturn("https://pay.stripe.com/receipts/ch_test_101");
        when(stripePaymentService.retrieveCharge("ch_test_101")).thenReturn(mockCharge);

        BalanceTransaction mockBt = mock(BalanceTransaction.class);
        when(mockBt.getFee()).thenReturn(44L); // 44 pence fee
        when(stripePaymentService.retrieveBalanceTransaction("txn_bal_101")).thenReturn(mockBt);

        stripeWebhookService.processWebhook("payload", "sig");

        // Verify donation state updated
        assertEquals(DonationPaymentStatus.SUCCEEDED, donation.getPaymentStatus());
        assertEquals(DonationSettlementStatus.SETTLED, donation.getSettlementStatus());
        assertEquals(44L, donation.getFeePence());
        assertEquals(956L, donation.getNetAmountPence());
        assertNotNull(donation.getSucceededAt());
        assertNotNull(donation.getSettledAt());

        // Verify ledger settlement was posted
        verify(fundLedgerService, times(1)).recordDonationSettlement(donation, 44L, "txn_bal_101");

        // Verify receipt was generated exactly once
        verify(donationReceiptService, times(1)).generateReceipt(eq(donation), anyString(), eq("ch_test_101"), eq("https://pay.stripe.com/receipts/ch_test_101"));

        // Verify payment event recorded
        verify(paymentEventRepository, times(1)).save(any(PaymentEvent.class));
    }

    @Test
    public void processWebhook_PaymentIntentFailed_OutOfOrderAfterSucceeded_Ignored() throws Exception {
        // Pre-condition: donation already succeeded
        donation.setPaymentStatus(DonationPaymentStatus.SUCCEEDED);
        donation.setSettlementStatus(DonationSettlementStatus.SETTLED);

        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("evt_failed_late");
        when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");

        PaymentIntent mockPi = mock(PaymentIntent.class);
        when(mockPi.getId()).thenReturn("pi_test_101");

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(mockPi));
        when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);

        when(stripePaymentService.constructWebhookEvent(anyString(), any(), any()))
                .thenReturn(mockEvent);
        when(paymentEventRepository.existsByStripeEventId("evt_failed_late")).thenReturn(false);
        when(donationRepository.findByStripePaymentIntentId("pi_test_101")).thenReturn(Optional.of(donation));

        stripeWebhookService.processWebhook("payload", "sig");

        // Verify status remains SUCCEEDED (not corrupted by out-of-order failure)
        assertEquals(DonationPaymentStatus.SUCCEEDED, donation.getPaymentStatus());
    }

    @Test
    public void processWebhook_ChargeRefunded_AdjustsLedger() throws Exception {
        donation.setPaymentStatus(DonationPaymentStatus.SUCCEEDED);
        donation.setSettlementStatus(DonationSettlementStatus.SETTLED);

        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("evt_refund_101");
        when(mockEvent.getType()).thenReturn("charge.refunded");

        Charge mockCharge = mock(Charge.class);
        when(mockCharge.getId()).thenReturn("ch_test_101");
        when(mockCharge.getPaymentIntent()).thenReturn("pi_test_101");
        when(mockCharge.getAmountRefunded()).thenReturn(1000L); // Full refund

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(mockCharge));
        when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);

        when(stripePaymentService.constructWebhookEvent(anyString(), any(), any()))
                .thenReturn(mockEvent);
        when(paymentEventRepository.existsByStripeEventId("evt_refund_101")).thenReturn(false);
        when(donationRepository.findByStripePaymentIntentId("pi_test_101")).thenReturn(Optional.of(donation));

        stripeWebhookService.processWebhook("payload", "sig");

        assertEquals(DonationPaymentStatus.REFUNDED, donation.getPaymentStatus());
        assertEquals(1000L, donation.getRefundedAmountPence());

        // Verify negative ledger entry posted
        verify(fundLedgerService, times(1)).recordDonationRefund(eq(donation), eq(1000L), eq("evt_refund_101"), anyString());
    }

    @Test
    public void donationReceiptService_UniquenessAndOwnership() {
        DonationReceiptServiceImpl receiptService = new DonationReceiptServiceImpl(donationReceiptRepository, donationRepository);

        donation.setPaymentStatus(DonationPaymentStatus.SUCCEEDED);

        // 1. First generation: creates new receipt
        when(donationReceiptRepository.findByDonationId(101L)).thenReturn(Optional.empty());
        when(donationReceiptRepository.save(any(DonationReceipt.class))).thenAnswer(i -> {
            DonationReceipt r = i.getArgument(0);
            r.setId(1L);
            return r;
        });

        DonationReceipt receipt1 = receiptService.generateReceipt(donation, "card", "ch_test_101", "https://receipt.url");
        assertNotNull(receipt1);
        assertTrue(receipt1.getReceiptNumber().startsWith("RCP-"));

        // 2. Second generation call: returns existing receipt (idempotency check)
        when(donationReceiptRepository.findByDonationId(101L)).thenReturn(Optional.of(receipt1));
        DonationReceipt receipt2 = receiptService.generateReceipt(donation, "card", "ch_test_101", "https://receipt.url");
        assertEquals(receipt1.getReceiptNumber(), receipt2.getReceiptNumber());

        // 3. Ownership check: requesting user must match donor
        when(donationRepository.findById(101L)).thenReturn(Optional.of(donation));

        // Intruding user (id 2) requests receipt for user 1's donation -> 403 Forbidden
        ApiException ex = assertThrows(ApiException.class, () ->
                receiptService.getReceiptForDonor(101L, otherUser.getId()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());

        // Owning user (id 1) requests receipt -> success
        DonationReceiptResponse res = receiptService.getReceiptForDonor(101L, donor.getId());
        assertNotNull(res);
        assertEquals(receipt1.getReceiptNumber(), res.getReceiptNumber());
        assertEquals(1000L, res.getAmountPence());
        assertEquals("£10.00", res.getAmountFormatted());
    }

    @Test
    public void fundLedgerService_BalanceFormulaAndSummary() {
        FundLedgerServiceImpl ledgerService = new FundLedgerServiceImpl(fundLedgerRepository);

        // Test Summary stats
        when(fundLedgerRepository.countSettledDonationEntries()).thenReturn(5L);
        when(fundLedgerRepository.sumSettledDonationGrossPence()).thenReturn(10000L); // £100.00 gross
        when(fundLedgerRepository.sumTotalFeesPence()).thenReturn(440L);             // £4.40 fees
        when(fundLedgerRepository.sumTotalRefundsGrossPence()).thenReturn(1000L);     // £10.00 refunds
        when(fundLedgerRepository.sumTotalAvailableBalancePence()).thenReturn(8560L); // £85.60 net

        FundSummaryResponse summary = ledgerService.getFundSummary();

        assertNotNull(summary);
        assertEquals("GBP", summary.getCurrency());
        assertEquals("£", summary.getCurrencySymbol());
        assertEquals(5L, summary.getTotalSettledDonationsCount());
        assertEquals(10000L, summary.getTotalGrossPence());
        assertEquals(440L, summary.getTotalFeesPence());
        assertEquals(1000L, summary.getTotalRefundsPence());
        assertEquals(8560L, summary.getAvailableBalancePence());
        assertEquals("£85.60", summary.getAvailableBalanceFormatted());
        assertTrue(summary.getFundPurpose().contains("Community Burial Fund"));
    }
}
