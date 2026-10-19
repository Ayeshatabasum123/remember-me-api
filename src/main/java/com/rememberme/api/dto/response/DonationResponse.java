package com.rememberme.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Donation details and status")
public class DonationResponse {

    @Schema(description = "Donation ID", example = "101")
    private Long id;

    @Schema(description = "Donation amount in integer pence", example = "1000")
    private Long amountPence;

    @Schema(description = "Formatted amount string", example = "£10.00")
    private String amountFormatted;

    @Schema(description = "Currency ISO code", example = "GBP")
    private String currency;

    @Schema(description = "Payment processing status", example = "SUCCEEDED")
    private String paymentStatus;

    @Schema(description = "Settlement status in community fund ledger", example = "SETTLED")
    private String settlementStatus;

    @Schema(description = "Donor full name", example = "Zainab Ahmed")
    private String donorName;

    @Schema(description = "Donor email address", example = "zainab@example.com")
    private String donorEmail;

    @Schema(description = "List of dedications linked to this donation")
    @Builder.Default
    private List<DedicationResponse> dedications = new ArrayList<>();

    @Schema(description = "Whether an official receipt is available for this donation", example = "true")
    private boolean hasReceipt;

    @Schema(description = "Official receipt number if available", example = "RCP-20261019-101001")
    private String receiptNumber;

    @Schema(description = "Timestamp when the donation was initiated")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the payment succeeded")
    private LocalDateTime succeededAt;

    @Schema(description = "Timestamp when the funds settled into the community fund ledger")
    private LocalDateTime settledAt;
}
