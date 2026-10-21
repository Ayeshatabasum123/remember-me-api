package com.rememberme.api.communityfund.controller;

import com.rememberme.api.service.StripeWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Community Burial Fund - Payment Webhook", description = "Verified asynchronous notifications and state updates from payment providers")
public class PaymentWebhookController {

    private final StripeWebhookService stripeWebhookService;

    @PostMapping("/webhook")
    @Operation(
            summary = "Handle Payment Provider Webhook",
            description = "Receives signed webhook notifications from the payment provider (e.g., Stripe-Signature). Authoritative source of truth for payment success, failure, cancellation, and refund events."
    )
    public ResponseEntity<Map<String, Object>> handlePaymentWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        log.info("Received payment provider webhook callback. Signature present: {}", sigHeader != null);
        stripeWebhookService.processWebhook(rawPayload, sigHeader);
        return ResponseEntity.ok(Map.of(
                "received", true,
                "message", "Payment webhook notification processed successfully"
        ));
    }
}
