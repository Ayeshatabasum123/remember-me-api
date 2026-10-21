package com.rememberme.api.communityfund.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Official receipt for a confirmed Community Burial Fund donation")
public class DonationReceiptInfoResponse {

    @Schema(description = "Official unique receipt number", example = "RCP-20261021-9F3C12")
    private String receiptNumber;

    @Schema(description = "Internal donation identifier", example = "101")
    private Long donationId;

    @Schema(description = "Donor full name", example = "John Doe")
    private String donorName;

    @Schema(description = "Donor email address", example = "john.doe@example.com")
    private String donorEmail;

    @Schema(description = "Confirmed donation amount", example = "50.00")
    private BigDecimal amount;

    @Schema(description = "Formatted confirmed amount", example = "£50.00")
    private String amountFormatted;

    @Schema(description = "Currency code", example = "GBP")
    private String currency;

    @Schema(description = "Payment method used (e.g., card, apple_pay)", example = "card")
    private String paymentMethod;

    @Schema(description = "Designated fund name", example = "Remember Me Community Burial Fund")
    private String fundName;

    @Schema(description = "Timestamp when receipt was officially issued")
    private LocalDateTime issuedAt;

    @Schema(description = "External hosted receipt URL from payment provider, if available")
    private String receiptUrl;
}
