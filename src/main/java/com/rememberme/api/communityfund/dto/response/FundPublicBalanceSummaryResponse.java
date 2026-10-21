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
@Schema(description = "Approved public balance summary for the shared Community Burial Fund")
public class FundPublicBalanceSummaryResponse {

    @Schema(description = "Standard ISO 4217 currency code", example = "GBP")
    private String currency;

    @Schema(description = "Currency symbol", example = "£")
    private String currencySymbol;

    @Schema(description = "Total number of confirmed settled donations", example = "124")
    private long totalConfirmedDonations;

    @Schema(description = "Total amount raised from confirmed donations", example = "6250.00")
    private BigDecimal totalAmountReceived;

    @Schema(description = "Formatted total amount raised", example = "£6,250.00")
    private String totalAmountReceivedFormatted;

    @Schema(description = "Total approved support funds disbursed directly to verified providers", example = "4500.00")
    private BigDecimal totalApprovedSupportDisbursed;

    @Schema(description = "Formatted total support funds disbursed", example = "£4,500.00")
    private String totalApprovedSupportDisbursedFormatted;

    @Schema(description = "Net available fund balance available for community burial assistance", example = "1750.00")
    private BigDecimal availableBalance;

    @Schema(description = "Formatted net available fund balance", example = "£1,750.00")
    private String availableBalanceFormatted;

    @Schema(description = "Timestamp when the public balance summary was calculated")
    private LocalDateTime asOf;
}
