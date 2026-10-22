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
@Schema(description = "Aggregated financial report metrics")
public class FundReportResponseDto {

    @Schema(description = "Total donations received in GBP", example = "50000.00")
    private BigDecimal donationTotals;

    @Schema(description = "Total refunds processed in GBP", example = "300.00")
    private BigDecimal refundTotals;

    @Schema(description = "Total burial provider disbursements in GBP", example = "15000.00")
    private BigDecimal providerPaymentTotals;

    @Schema(description = "Currently reserved funds in GBP", example = "3000.00")
    private BigDecimal reservedFunds;

    @Schema(description = "Available unreserved fund balance in GBP", example = "30500.00")
    private BigDecimal availableBalance;

    @Schema(description = "Number of completed burial assistance cases", example = "12")
    private Long completedAssistanceCases;

    @Schema(description = "Total number of donation transactions settled", example = "250")
    private Long totalDonors;

    @Schema(description = "Currency", example = "GBP")
    @Builder.Default
    private String currency = "GBP";
}
