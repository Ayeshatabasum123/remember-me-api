package com.rememberme.api.communityfund.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Predefined donation tiers, limits, and shared fund explanation")
public class DonationAmountsResponse {

    @Schema(description = "Standard ISO currency code", example = "GBP")
    private String currency;

    @Schema(description = "Currency symbol", example = "£")
    private String currencySymbol;

    @Schema(description = "List of predefined donation options")
    private List<PresetAmountOption> presets;

    @Schema(description = "Custom donation amount rules and bounds")
    private CustomAmountConfig customAmount;

    @Schema(description = "Explanation of how donations flow into the shared community fund")
    private String explanation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Preset donation tier")
    public static class PresetAmountOption {
        @Schema(description = "Preset numerical amount", example = "50.00")
        private BigDecimal amount;

        @Schema(description = "Formatted amount string", example = "£50.00")
        private String formatted;

        @Schema(description = "Community impact description for this tier", example = "Substantially relieves burial poverty for families in hardship")
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Custom amount configuration")
    public static class CustomAmountConfig {
        @Schema(description = "Whether custom donation amounts are allowed", example = "true")
        private boolean enabled;

        @Schema(description = "Minimum allowed donation amount", example = "1.00")
        private BigDecimal minAmount;

        @Schema(description = "Maximum allowed donation amount", example = "50000.00")
        private BigDecimal maxAmount;

        @Schema(description = "Formatted minimum amount", example = "£1.00")
        private String minAmountFormatted;

        @Schema(description = "Formatted maximum amount", example = "£50,000.00")
        private String maxAmountFormatted;
    }
}
