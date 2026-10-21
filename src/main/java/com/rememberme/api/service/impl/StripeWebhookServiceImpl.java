package com.rememberme.api.service.impl;

import com.rememberme.api.config.StripeConfig;
import com.rememberme.api.entity.Donation;
import com.rememberme.api.entity.DonationPaymentStatus;
import com.rememberme.api.entity.DonationSettlementStatus;
import com.rememberme.api.entity.PaymentEvent;
import com.rememberme.api.entity.PaymentEventStatus;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.DonationRepository;
import com.rememberme.api.repository.PaymentEventRepository;
import com.rememberme.api.service.DonationReceiptService;
import com.rememberme.api.service.FundLedgerService;
import com.rememberme.api.service.StripePaymentService;
import com.rememberme.api.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.BalanceTransaction;
import com.stripe.model.Charge;
import com.stripe.model.Dispute;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import com.rememberme.api.communityfund.entity.PaymentTransaction;
import com.rememberme.api.communityfund.repository.PaymentTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookServiceImpl implements StripeWebhookService {

    private final StripeConfig stripeConfig;
    private final StripePaymentService stripePaymentService;
    private final PaymentEventRepository paymentEventRepository;
    private final DonationRepository donationRepository;
    private final DonationReceiptService donationReceiptService;
    private final FundLedgerService fundLedgerService;

    @Autowired(required = false)
    private PaymentTransactionRepository paymentTransactionRepository;

    @Override
    @Transactional
    public void processWebhook(String payload, String sigHeader) {
        if (payload == null || payload.isBlank()) {
            throw new ApiException("Webhook payload cannot be empty", HttpStatus.BAD_REQUEST);
        }

        Event event;
        try {
            event = stripePaymentService.constructWebhookEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed: {}", e.getMessage());
            throw new ApiException("Invalid Stripe signature: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error parsing Stripe webhook: {}", e.getMessage(), e);
            throw new ApiException("Webhook processing error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        String eventId = event.getId();
        log.info("Received Stripe webhook event: [id={}, type={}]", eventId, event.getType());

        // 1. Idempotency Check: Prevent duplicate event processing
        if (paymentEventRepository.existsByStripeEventId(eventId)) {
            log.info("Stripe event {} has already been processed. Returning 200 OK idempotently.", eventId);
            return;
        }

        // Record the event in payment_events table
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .stripeEventId(eventId)
                .eventType(event.getType())
                .status(PaymentEventStatus.PROCESSED)
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event, paymentEvent);
                    break;

                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event, paymentEvent);
                    break;

                case "payment_intent.canceled":
                    handlePaymentIntentCanceled(event, paymentEvent);
                    break;

                case "charge.refunded":
                    handleChargeRefunded(event, paymentEvent);
                    break;

                case "charge.dispute.created":
                    handleDisputeCreated(event, paymentEvent);
                    break;

                case "charge.dispute.closed":
                    handleDisputeClosed(event, paymentEvent);
                    break;

                default:
                    log.info("Unhandled Stripe webhook event type [{}], acknowledging safely.", event.getType());
                    paymentEvent.setStatus(PaymentEventStatus.IGNORED);
                    break;
            }

            try {
                paymentEventRepository.save(paymentEvent);
            } catch (DataIntegrityViolationException dive) {
                log.info("Concurrent webhook processing detected for event {}. Acknowledging.", eventId);
            }
        } catch (Exception ex) {
            log.error("Error handling Stripe event [{}]: {}", eventId, ex.getMessage(), ex);
            paymentEvent.setStatus(PaymentEventStatus.FAILED);
            paymentEvent.setErrorMessage(ex.getMessage());
            try {
                paymentEventRepository.save(paymentEvent);
            } catch (Exception ignored) {}
            throw ex;
        }
    }

    private void handlePaymentIntentSucceeded(Event event, PaymentEvent paymentEvent) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        PaymentIntent paymentIntent = (PaymentIntent) deserializer.getObject()
                .orElseThrow(() -> new IllegalStateException("Unable to deserialize PaymentIntent from event " + event.getId()));

        String piId = paymentIntent.getId();
        paymentEvent.setStripePaymentIntentId(piId);

        Donation donation = findDonationByPaymentIntent(paymentIntent);
        if (donation == null) {
            log.warn("No donation found for Stripe PaymentIntent: {}", piId);
            return;
        }

        paymentEvent.setStripePaymentIntentId(donation.getStripePaymentIntentId());

        // Extract Charge ID and receipt URL
        String chargeId = paymentIntent.getLatestCharge();
        String receiptUrl = null;
        String paymentMethod = "card";
        Long feePence = 0L;
        String balanceTxId = null;

        if (chargeId != null && !chargeId.isBlank()) {
            paymentEvent.setStripeChargeId(chargeId);
            donation.setStripeChargeId(chargeId);
            try {
                Charge charge = stripePaymentService.retrieveCharge(chargeId);
                if (charge != null) {
                    receiptUrl = charge.getReceiptUrl();
                    if (charge.getPaymentMethodDetails() != null && charge.getPaymentMethodDetails().getType() != null) {
                        paymentMethod = charge.getPaymentMethodDetails().getType();
                    }
                    balanceTxId = charge.getBalanceTransaction();
                }
            } catch (Exception e) {
                log.warn("Could not retrieve charge details for {}: {}", chargeId, e.getMessage());
            }
        }

        // Reconcile balance transaction and fee if balance transaction is present
        if (balanceTxId != null && !balanceTxId.isBlank()) {
            donation.setStripeBalanceTransactionId(balanceTxId);
            try {
                BalanceTransaction bt = stripePaymentService.retrieveBalanceTransaction(balanceTxId);
                if (bt != null && bt.getFee() != null) {
                    feePence = bt.getFee();
                    log.info("Reconciled Stripe fee: {} pence from balance transaction {}", feePence, balanceTxId);
                }
            } catch (Exception e) {
                log.warn("Could not retrieve balance transaction {}: {}", balanceTxId, e.getMessage());
            }
        }

        // Check if already succeeded (out-of-order or duplicate guard)
        if (donation.getPaymentStatus() == DonationPaymentStatus.SUCCEEDED &&
                donation.getSettlementStatus() == DonationSettlementStatus.SETTLED) {
            log.info("Donation {} is already succeeded and settled. Ensuring receipt and ledger entry exist.", donation.getId());
            donationReceiptService.generateReceipt(donation, paymentMethod, chargeId, receiptUrl);
            fundLedgerService.recordDonationSettlement(donation, feePence, balanceTxId);
            return;
        }

        // Update donation status
        donation.setPaymentStatus(DonationPaymentStatus.SUCCEEDED);
        donation.setSucceededAt(LocalDateTime.now());
        donation.setFeePence(feePence);
        donation.setNetAmountPence(donation.getAmountPence() - feePence);
        donation.setSettlementStatus(DonationSettlementStatus.SETTLED);
        donation.setSettledAt(LocalDateTime.now());

        donationRepository.save(donation);

        // 1. Post to immutable fund ledger
        fundLedgerService.recordDonationSettlement(donation, feePence, balanceTxId);

        // 2. Generate official donation receipt exactly once
        donationReceiptService.generateReceipt(donation, paymentMethod, chargeId, receiptUrl);

        // 3. Record transaction audit
        recordTransaction(donation, piId, donation.getAmountPence(), "SUCCESS", "PAYMENT_CONFIRMED");

        log.info("Successfully processed payment_intent.succeeded for donationId: {}", donation.getId());
    }

    private void handlePaymentIntentFailed(Event event, PaymentEvent paymentEvent) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        PaymentIntent paymentIntent = (PaymentIntent) deserializer.getObject()
                .orElseThrow(() -> new IllegalStateException("Unable to deserialize PaymentIntent"));

        Donation donation = findDonationByPaymentIntent(paymentIntent);
        if (donation == null) return;

        paymentEvent.setStripePaymentIntentId(donation.getStripePaymentIntentId());

        // Out-of-order guard: do not mark as failed if already succeeded or refunded
        if (donation.getPaymentStatus() == DonationPaymentStatus.SUCCEEDED ||
                donation.getPaymentStatus() == DonationPaymentStatus.REFUNDED) {
            log.warn("Ignoring payment_intent.payment_failed for donation {} because status is already {}",
                    donation.getId(), donation.getPaymentStatus());
            return;
        }

        donation.setPaymentStatus(DonationPaymentStatus.FAILED);
        donationRepository.save(donation);
        recordTransaction(donation, paymentIntent.getId(), donation.getAmountPence(), "FAILED", "PAYMENT_FAILED");
        log.info("Marked donation {} as FAILED", donation.getId());
    }

    private void handlePaymentIntentCanceled(Event event, PaymentEvent paymentEvent) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        PaymentIntent paymentIntent = (PaymentIntent) deserializer.getObject()
                .orElseThrow(() -> new IllegalStateException("Unable to deserialize PaymentIntent"));

        Donation donation = findDonationByPaymentIntent(paymentIntent);
        if (donation == null) return;

        paymentEvent.setStripePaymentIntentId(donation.getStripePaymentIntentId());

        if (donation.getPaymentStatus() == DonationPaymentStatus.SUCCEEDED) {
            log.warn("Ignoring payment_intent.canceled for succeeded donation {}", donation.getId());
            return;
        }

        donation.setPaymentStatus(DonationPaymentStatus.CANCELLED);
        donationRepository.save(donation);
        recordTransaction(donation, paymentIntent.getId(), donation.getAmountPence(), "CANCELLED", "PAYMENT_CANCELLED");
        log.info("Marked donation {} as CANCELLED", donation.getId());
    }

    private void handleChargeRefunded(Event event, PaymentEvent paymentEvent) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Charge charge = (Charge) deserializer.getObject()
                .orElseThrow(() -> new IllegalStateException("Unable to deserialize Charge"));

        paymentEvent.setStripeChargeId(charge.getId());

        Donation donation = null;
        if (charge.getPaymentIntent() != null) {
            donation = donationRepository.findByStripePaymentIntentId(charge.getPaymentIntent()).orElse(null);
        }
        if (donation == null) {
            donation = donationRepository.findByStripeChargeId(charge.getId()).orElse(null);
        }
        if (donation == null) {
            log.warn("No donation found for refunded charge: {}", charge.getId());
            return;
        }

        long amountRefundedPence = charge.getAmountRefunded() != null ? charge.getAmountRefunded() : donation.getAmountPence();
        donation.setRefundedAmountPence(amountRefundedPence);

        if (amountRefundedPence >= donation.getAmountPence()) {
            donation.setPaymentStatus(DonationPaymentStatus.REFUNDED);
        } else {
            donation.setPaymentStatus(DonationPaymentStatus.PARTIALLY_REFUNDED);
        }

        donationRepository.save(donation);

        // Record negative adjustment in the fund ledger
        String refundId = event.getId();
        fundLedgerService.recordDonationRefund(donation, amountRefundedPence, refundId, "Stripe Charge Refund");
        recordTransaction(donation, charge.getId(), amountRefundedPence, "REFUNDED", "PAYMENT_REFUNDED");
        log.info("Recorded refund of {} pence for donationId: {}", amountRefundedPence, donation.getId());
    }

    private void recordTransaction(Donation donation, String providerTxId, long amountPence, String status, String txType) {
        if (paymentTransactionRepository != null && donation != null) {
            try {
                paymentTransactionRepository.save(PaymentTransaction.builder()
                        .donation(donation)
                        .providerTransactionId(providerTxId)
                        .amount(BigDecimal.valueOf(amountPence, 2))
                        .amountPence(amountPence)
                        .currency(donation.getCurrency() != null ? donation.getCurrency() : "GBP")
                        .status(status)
                        .transactionType(txType)
                        .createdAt(LocalDateTime.now())
                        .build());
            } catch (Exception e) {
                log.warn("Could not record payment transaction audit for donation {}: {}", donation.getId(), e.getMessage());
            }
        }
    }

    private void handleDisputeCreated(Event event, PaymentEvent paymentEvent) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Dispute dispute = (Dispute) deserializer.getObject()
                .orElseThrow(() -> new IllegalStateException("Unable to deserialize Dispute"));

        String chargeId = dispute.getCharge();
        paymentEvent.setStripeChargeId(chargeId);

        Donation donation = donationRepository.findByStripeChargeId(chargeId).orElse(null);
        if (donation == null) {
            log.warn("No donation found for disputed charge: {}", chargeId);
            return;
        }

        donation.setPaymentStatus(DonationPaymentStatus.DISPUTED);
        donationRepository.save(donation);

        long disputedAmountPence = dispute.getAmount() != null ? dispute.getAmount() : donation.getAmountPence();
        fundLedgerService.recordDisputeAdjustment(donation, disputedAmountPence, 1500L, dispute.getId()); // £15 standard dispute fee
        log.info("Recorded dispute adjustment for donationId: {}", donation.getId());
    }

    private void handleDisputeClosed(Event event, PaymentEvent paymentEvent) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Dispute dispute = (Dispute) deserializer.getObject()
                .orElseThrow(() -> new IllegalStateException("Unable to deserialize Dispute"));

        String chargeId = dispute.getCharge();
        Donation donation = donationRepository.findByStripeChargeId(chargeId).orElse(null);
        if (donation == null) return;

        if ("won".equalsIgnoreCase(dispute.getStatus())) {
            log.info("Dispute won for donationId: {}. Restoring status.", donation.getId());
            donation.setPaymentStatus(DonationPaymentStatus.SUCCEEDED);
            donationRepository.save(donation);
            // Re-credit the disputed amount
            long disputedAmountPence = dispute.getAmount() != null ? dispute.getAmount() : donation.getAmountPence();
            fundLedgerService.recordDonationSettlement(donation, 0L, "DISPUTE-WON-" + dispute.getId());
        } else {
            log.info("Dispute lost for donationId: {}", donation.getId());
        }
    }

    private Donation findDonationByPaymentIntent(PaymentIntent paymentIntent) {
        String piId = paymentIntent.getId();
        Optional<Donation> opt = donationRepository.findByStripePaymentIntentId(piId);
        if (opt.isPresent()) {
            return opt.get();
        }

        // Fallback: check metadata
        if (paymentIntent.getMetadata() != null && paymentIntent.getMetadata().containsKey("donation_id")) {
            try {
                Long donationId = Long.parseLong(paymentIntent.getMetadata().get("donation_id"));
                Optional<Donation> byId = donationRepository.findById(donationId);
                if (byId.isPresent()) {
                    Donation d = byId.get();
                    d.setStripePaymentIntentId(piId);
                    return donationRepository.save(d);
                }
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
