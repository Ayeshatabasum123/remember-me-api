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
@Schema(description = "Aggregated, public impact statistics for the Community Burial Fund")
public class FundImpactResponse {

    @Schema(description = "Standard ISO 4217 currency code", example = "GBP")
    private String currency;

    @Schema(description = "Currency symbol", example = "£")
    private String currencySymbol;

    @Schema(description = "Total number of confirmed, settled donations", example = "124")
    private long totalConfirmedDonations;

    @Schema(description = "Total monetary amount received from confirmed donations in major currency units", example = "6250.00")
    private BigDecimal totalAmountReceived;

    @Schema(description = "Formatted total monetary amount received", example = "£6,250.00")
    private String totalAmountReceivedFormatted;

    @Schema(description = "Total completed burial support cases successfully assisted", example = "8")
    private long totalCompletedSupportCases;

    @Schema(description = "Total approved support funds disbursed directly to verified providers", example = "4500.00")
    private BigDecimal totalApprovedSupportDisbursed;

    @Schema(description = "Formatted total support funds disbursed", example = "£4,500.00")
    private String totalApprovedSupportDisbursedFormatted;

    @Schema(description = "Timestamp when impact figures were calculated from authoritative records")
    private LocalDateTime asOf;
}
