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
@Schema(description = "Aggregated public summary of the Community Burial Fund")
public class FundSummaryResponse {

    @Schema(description = "Currency code", example = "GBP")
    @Builder.Default
    private String currency = "GBP";

    @Schema(description = "Currency symbol", example = "£")
    @Builder.Default
    private String currencySymbol = "£";

    @Schema(description = "Total count of confirmed, settled donations", example = "142")
    private Long totalSettledDonationsCount;

    @Schema(description = "Total gross donations received in pence", example = "750000")
    private Long totalGrossPence;

    @Schema(description = "Total payment processing fees incurred in pence", example = "21500")
    private Long totalFeesPence;

    @Schema(description = "Total refunds and dispute deductions in pence", example = "0")
    private Long totalRefundsPence;

    @Schema(description = "Current available fund balance in integer pence (gross minus fees minus refunds/disbursements)", example = "728500")
    private Long availableBalancePence;

    @Schema(description = "Formatted current available fund balance string", example = "£7,285.00")
    private String availableBalanceFormatted;

    @Schema(description = "Purpose and mission statement of the Community Burial Fund")
    private String fundPurpose;

    @Schema(description = "Timestamp when this summary was generated")
    private LocalDateTime generatedAt;
}
