package com.rememberme.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Donation configuration options and preset values for the Community Burial Fund")
public class DonationOptionsResponse {

    @Schema(description = "Currency code", example = "GBP")
    @Builder.Default
    private String currency = "GBP";

    @Schema(description = "Currency symbol", example = "£")
    @Builder.Default
    private String currencySymbol = "£";

    @Schema(description = "Pre-configured donation amount tiers in pence")
    private List<PresetAmountDto> presetAmounts;

    @Schema(description = "Minimum allowed custom donation in pence", example = "100")
    private Long minCustomAmountPence;

    @Schema(description = "Maximum allowed custom donation in pence", example = "500000")
    private Long maxCustomAmountPence;

    @Schema(description = "User-friendly explanation of the shared Community Burial Fund and dedications model")
    private String sharedFundExplanation;

    @Schema(description = "Stripe publishable key for Flutter PaymentSheet initialization", example = "pk_test_...")
    private String stripePublishableKey;
}
