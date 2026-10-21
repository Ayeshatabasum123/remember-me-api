package com.rememberme.api.communityfund.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed status and record of a donation")
public class DonationDetailsResponse {

    @Schema(description = "Internal unique donation ID", example = "101")
    private Long id;

    @Schema(description = "Donor display name", example = "John Doe")
    private String donorName;

    @Schema(description = "Donor email address", example = "john.doe@example.com")
    private String donorEmail;

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

    @Schema(description = "Payment provider used", example = "STRIPE")
    private String paymentProvider;

    @Schema(description = "Provider payment reference identifier", example = "pi_12345")
    private String providerPaymentId;

    @Schema(description = "Client secret if payment is still pending or processing", example = "pi_12345_secret_67890")
    private String clientSecret;

    @Schema(description = "Dedicated loved one names", example = "[\"John Smith\"]")
    private List<String> lovedOneNames;

    @Schema(description = "Dedication message tribute", example = "In loving memory")
    private String dedicationMessage;

    @Schema(description = "Whether official receipt is generated and accessible", example = "false")
    private boolean receiptAvailable;

    @Schema(description = "Official receipt number if confirmed", example = "RCP-20261021-ABC123")
    private String receiptNumber;

    @Schema(description = "Timestamp when donation was created")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when donation was updated or succeeded")
    private LocalDateTime updatedAt;
}
