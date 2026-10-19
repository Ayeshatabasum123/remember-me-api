package com.rememberme.api.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.BalanceTransaction;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;

public interface StripePaymentService {

    /**
     * Creates a Stripe PaymentIntent for a Community Burial Fund donation.
     * Attaches ONLY internal donationId in metadata. Never exposes personal details to Stripe metadata.
     */
    PaymentIntent createPaymentIntent(Long amountPence, String currency, Long donationId, String donorEmail, String idempotencyKey) throws StripeException;

    /**
     * Retrieves an existing PaymentIntent from Stripe.
     */
    PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException;

    /**
     * Retrieves a Stripe BalanceTransaction to reconcile actual fee and net settlement.
     */
    BalanceTransaction retrieveBalanceTransaction(String balanceTransactionId) throws StripeException;

    /**
     * Retrieves a Stripe Charge to inspect receipt URL, payment method details, and balance transaction ID.
     */
    Charge retrieveCharge(String chargeId) throws StripeException;

    /**
     * Verifies Stripe webhook signature and constructs the typed Stripe Event.
     */
    Event constructWebhookEvent(String payload, String sigHeader, String webhookSecret) throws SignatureVerificationException;
}
