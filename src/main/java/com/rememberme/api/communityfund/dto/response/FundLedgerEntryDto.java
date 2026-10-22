package com.rememberme.api.communityfund.dto.response;

import com.rememberme.api.entity.FundLedgerEntryType;
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
@Schema(description = "Immutable fund ledger transaction record")
public class FundLedgerEntryDto {

    @Schema(description = "Ledger entry ID", example = "100")
    private Long id;

    @Schema(description = "Transaction entry type", example = "DONATION_SETTLEMENT")
    private FundLedgerEntryType entryType;

    @Schema(description = "Associated donation ID if applicable", example = "50")
    private Long donationId;

    @Schema(description = "Gross amount in GBP", example = "50.00")
    private BigDecimal grossAmount;

    @Schema(description = "Payment fee in GBP", example = "1.50")
    private BigDecimal fee;

    @Schema(description = "Net ledger amount credited or debited in GBP", example = "48.50")
    private BigDecimal netAmount;

    @Schema(description = "Post-transaction running balance in GBP", example = "1048.50")
    private BigDecimal runningBalance;

    @Schema(description = "Currency", example = "GBP")
    private String currency;

    @Schema(description = "Unique audit reference", example = "DON-SETTLE-50")
    private String reference;

    @Schema(description = "Transaction description", example = "Settled donation from Stripe")
    private String description;

    @Schema(description = "Ledger posting timestamp", example = "2026-10-22T12:00:00")
    private LocalDateTime createdAt;
}
