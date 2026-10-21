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
@Schema(description = "Response returned upon initiating donation checkout with payment provider")
public class DonationCheckoutResponse {

    @Schema(description = "Internal unique donation ID", example = "101")
    private Long donationId;

    @Schema(description = "Client secret for Stripe PaymentSheet or Stripe Elements checkout", example = "pi_12345_secret_67890")
    private String clientSecret;

    @Schema(description = "Provider payment intent identifier", example = "pi_12345")
    private String stripePaymentIntentId;

    @Schema(description = "Payment provider used", example = "STRIPE")
    private String paymentProvider;

    @Schema(description = "Donation amount", example = "50.00")
    private BigDecimal amount;

    @Schema(description = "Formatted donation amount", example = "£50.00")
    private String amountFormatted;

    @Schema(description = "Currency code", example = "GBP")
    private String currency;

    @Schema(description = "Currency symbol", example = "£")
    private String currencySymbol;

    @Schema(description = "Current payment status", example = "PENDING")
    private String paymentStatus;

    @Schema(description = "Committed loved one names in dedication", example = "[\"John Smith\"]")
    private List<String> dedicationNames;

    @Schema(description = "Dedication message tribute", example = "In loving memory")
    private String dedicationMessage;

    @Schema(description = "Instructions for mobile app / frontend checkout completion", example = "Pass clientSecret to Stripe Mobile PaymentSheet or Web Elements.")
    private String checkoutInstructions;
}
