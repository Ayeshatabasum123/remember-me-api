package com.rememberme.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payment intent response containing client secret for Stripe PaymentSheet")
public class CreatePaymentIntentResponse {

    @Schema(description = "Internal Remember Me donation ID", example = "101")
    private Long donationId;

    @Schema(description = "Stripe PaymentIntent client secret used by Flutter Stripe PaymentSheet", example = "pi_3Mtwx72eZvKYlo2C01n0D..._secret_...")
    private String clientSecret;

    @Schema(description = "Stripe PaymentIntent ID", example = "pi_3Mtwx72eZvKYlo2C01n0D")
    private String paymentIntentId;

    @Schema(description = "Donation amount in integer pence", example = "1000")
    private Long amountPence;

    @Schema(description = "Currency code", example = "GBP")
    private String currency;

    @Schema(description = "Current payment status of the donation", example = "PENDING")
    private String status;

    @Schema(description = "Idempotency key associated with this payment intent", example = "idem-d4f7b2-1000")
    private String idempotencyKey;
}
