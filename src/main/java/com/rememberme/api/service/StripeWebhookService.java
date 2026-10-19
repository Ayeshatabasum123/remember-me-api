package com.rememberme.api.service;

public interface StripeWebhookService {

    /**
     * Processes raw Stripe webhook payload and signature header.
     * Verifies cryptographic signature, prevents duplicate event execution,
     * reconciles settlement, issues receipts, and adjusts ledger on refunds.
     */
    void processWebhook(String payload, String sigHeader);
}
