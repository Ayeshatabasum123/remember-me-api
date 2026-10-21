package com.rememberme.api.communityfund.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to initiate donation checkout and payment intent creation")
public class DonationCheckoutRequest {

    @NotNull(message = "Donation amount is required")
    @DecimalMin(value = "1.00", message = "Donation amount must be at least £1.00")
    @Schema(description = "Donation amount in GBP", example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Builder.Default
    @Schema(description = "Currency code (locked to GBP)", example = "GBP", defaultValue = "GBP")
    private String currency = "GBP";

    @Valid
    @Schema(description = "Optional dedication in memory of loved ones")
    private DedicationRequest dedication;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Optional commemorative tribute/dedication details")
    public static class DedicationRequest {

        @Builder.Default
        @Schema(description = "Whether dedication is enabled", example = "true")
        private boolean enabled = true;

        @Schema(description = "Names of loved ones being remembered", example = "[\"John Smith\", \"Mary Smith\"]")
        private List<String> lovedOneNames;

        @Schema(description = "Single loved one name (alternative for convenience)", example = "John Smith")
        private String lovedOneName;

        @Schema(description = "Optional tribute or prayer message", example = "In loving memory")
        private String message;

        public List<String> resolveLovedOneNames() {
            List<String> result = new ArrayList<>();
            if (lovedOneNames != null) {
                for (String name : lovedOneNames) {
                    if (name != null && !name.trim().isEmpty()) {
                        result.add(name.trim());
                    }
                }
            }
            if (result.isEmpty() && lovedOneName != null && !lovedOneName.trim().isEmpty()) {
                result.add(lovedOneName.trim());
            }
            return result;
        }
    }
}
