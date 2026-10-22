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
@Schema(description = "Summary of fund account balances and obligations")
public class FundBalanceResponseDto {

    @Schema(description = "Total settled donations received gross in GBP", example = "50000.00")
    private BigDecimal totalDonationsReceived;

    @Schema(description = "Total payment processing fees deducted in GBP", example = "1200.00")
    private BigDecimal totalProcessingFees;

    @Schema(description = "Total refunds processed in GBP", example = "300.00")
    private BigDecimal totalRefunds;

    @Schema(description = "Total confirmed burial provider disbursements in GBP", example = "15000.00")
    private BigDecimal totalDisbursements;

    @Schema(description = "Active committed fund reservations in GBP", example = "3000.00")
    private BigDecimal activeReservations;

    @Schema(description = "Unreserved available fund balance in GBP", example = "30500.00")
    private BigDecimal availableFundBalance;

    @Schema(description = "Currency", example = "GBP")
    @Builder.Default
    private String currency = "GBP";
}
