package com.rememberme.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to initiate a Community Burial Fund donation payment intent via Stripe")
public class CreateDonationPaymentIntentRequest {

    @NotNull(message = "amountPence is required")
    @Min(value = 100, message = "Minimum donation amount is £1.00 (100 pence)")
    @Max(value = 500000, message = "Maximum donation amount is £5,000.00 (500000 pence)")
    @Schema(description = "Donation amount in integer pence (e.g., 500 for £5.00, 1000 for £10.00, 5000 for £50.00)", example = "1000")
    private Long amountPence;

    @Schema(description = "Currency ISO code, defaults to GBP", example = "GBP")
    @Builder.Default
    private String currency = "GBP";

    @Schema(description = "Client-supplied idempotency key to prevent accidental duplicate requests", example = "idem-d4f7b2-1000")
    private String idempotencyKey;

    @Valid
    @Schema(description = "Optional list of dedications honoring loved ones. Dedications do not reserve or allocate money to individuals.")
    @Builder.Default
    private List<DedicationRequest> dedications = new ArrayList<>();
}
