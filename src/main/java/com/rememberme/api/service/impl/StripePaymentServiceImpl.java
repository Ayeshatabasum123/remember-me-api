package com.rememberme.api.service.impl;

import com.rememberme.api.config.StripeConfig;
import com.rememberme.api.service.StripePaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.BalanceTransaction;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentServiceImpl implements StripePaymentService {

    private final StripeConfig stripeConfig;

    @Override
    public PaymentIntent createPaymentIntent(Long amountPence, String currency, Long donationId, String donorEmail, String idempotencyKey) throws StripeException {
        RequestOptions.RequestOptionsBuilder optionsBuilder = RequestOptions.builder()
                .setApiKey(stripeConfig.getSecretKey());

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            optionsBuilder.setIdempotencyKey(idempotencyKey);
        }

        RequestOptions requestOptions = optionsBuilder.build();

        // Security requirement: Put ONLY internal donation ID in Stripe metadata.
        // Never send loved-one names, memorial text, or private support details to Stripe.
        Map<String, String> metadata = new HashMap<>();
        metadata.put("donation_id", String.valueOf(donationId));

        PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(amountPence)
                .setCurrency(currency != null ? currency.toLowerCase() : "gbp")
                .setDescription("Community Burial Fund Donation #" + donationId)
                .putAllMetadata(metadata)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                );

        if (donorEmail != null && !donorEmail.isBlank()) {
            paramsBuilder.setReceiptEmail(donorEmail);
        }

        log.info("Creating Stripe PaymentIntent for donationId: {}, amount: {} {}", donationId, amountPence, currency);
        return PaymentIntent.create(paramsBuilder.build(), requestOptions);
    }

    @Override
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        RequestOptions requestOptions = RequestOptions.builder()
                .setApiKey(stripeConfig.getSecretKey())
                .build();
        return PaymentIntent.retrieve(paymentIntentId, requestOptions);
    }

    @Override
    public BalanceTransaction retrieveBalanceTransaction(String balanceTransactionId) throws StripeException {
        RequestOptions requestOptions = RequestOptions.builder()
                .setApiKey(stripeConfig.getSecretKey())
                .build();
        return BalanceTransaction.retrieve(balanceTransactionId, requestOptions);
    }

    @Override
    public Charge retrieveCharge(String chargeId) throws StripeException {
        RequestOptions requestOptions = RequestOptions.builder()
                .setApiKey(stripeConfig.getSecretKey())
                .build();
        return Charge.retrieve(chargeId, requestOptions);
    }

    @Override
    public Event constructWebhookEvent(String payload, String sigHeader, String webhookSecret) throws SignatureVerificationException {
        return Webhook.constructEvent(payload, sigHeader, webhookSecret);
    }
}
