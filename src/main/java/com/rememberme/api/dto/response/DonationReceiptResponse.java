package com.rememberme.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Official donation receipt for donor record keeping")
public class DonationReceiptResponse {

    @Schema(description = "Official receipt unique identifier", example = "RCP-20261019-101001")
    private String receiptNumber;

    @Schema(description = "Internal donation ID", example = "101")
    private Long donationId;

    @Schema(description = "Donor full name", example = "Zainab Ahmed")
    private String donorName;

    @Schema(description = "Donor email address", example = "zainab@example.com")
    private String donorEmail;

    @Schema(description = "Total donation amount in integer pence", example = "1000")
    private Long amountPence;

    @Schema(description = "Formatted amount string", example = "£10.00")
    private String amountFormatted;

    @Schema(description = "Currency ISO code", example = "GBP")
    private String currency;

    @Schema(description = "Payment method used", example = "card")
    private String paymentMethod;

    @Schema(description = "Beneficiary fund name", example = "Community Burial Fund")
    private String fundName;

    @Schema(description = "Optional external receipt or hosted receipt URL", example = "https://pay.stripe.com/receipts/...")
    private String receiptUrl;

    @Schema(description = "Timestamp when receipt was issued")
    private LocalDateTime issuedAt;
}
