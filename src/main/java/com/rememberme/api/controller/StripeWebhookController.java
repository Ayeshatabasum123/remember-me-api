package com.rememberme.api.controller;

import com.rememberme.api.service.StripeWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stripe Webhooks", description = "Raw Stripe webhook event receiver for asynchronous payment verification")
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    @PostMapping("/stripe")
    @Operation(summary = "Handle Stripe Webhook Events", description = "Receives raw Stripe webhook payload with Stripe-Signature verification. Authoritative source of truth for payment success, failure, refund, and dispute states.")
    public ResponseEntity<Map<String, Object>> handleStripeWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        log.info("Received Stripe webhook call. Signature header present: {}", sigHeader != null);
        stripeWebhookService.processWebhook(rawPayload, sigHeader);
        return ResponseEntity.ok(Map.of(
                "received", true,
                "message", "Webhook event processed successfully"
        ));
    }
}
