package com.rememberme.api.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Slf4j
public class StripeConfig {

    @Value("${stripe.secret-key:sk_test_placeholder}")
    private String secretKey;

    @Value("${stripe.publishable-key:pk_test_placeholder}")
    private String publishableKey;

    @Value("${stripe.webhook-secret:whsec_placeholder}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        if (secretKey != null && !secretKey.isBlank() && !secretKey.contains("placeholder")) {
            Stripe.apiKey = secretKey;
            log.info("Stripe SDK initialized with configured API key.");
        } else {
            Stripe.apiKey = secretKey;
            log.warn("Stripe SDK initialized with placeholder API key. Set STRIPE_SECRET_KEY for live/test Stripe calls.");
        }
    }
}
