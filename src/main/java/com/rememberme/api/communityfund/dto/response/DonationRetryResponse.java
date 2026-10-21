package com.rememberme.api.communityfund.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response returned upon safely retrying a failed or expired donation payment")
public class DonationRetryResponse {

    @Schema(description = "Existing donation identifier", example = "101")
    private Long donationId;

    @Schema(description = "New or refreshed client secret for completing checkout", example = "pi_12345_secret_fresh")
    private String clientSecret;

    @Schema(description = "Provider payment intent identifier", example = "pi_12345")
    private String stripePaymentIntentId;

    @Schema(description = "Donation amount", example = "50.00")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "GBP")
    private String currency;

    @Schema(description = "Updated status of the donation", example = "PENDING")
    private String status;

    @Schema(description = "Helpful status message", example = "Payment checkout session successfully refreshed. You can now complete payment safely without creating a duplicate record.")
    private String message;
}
