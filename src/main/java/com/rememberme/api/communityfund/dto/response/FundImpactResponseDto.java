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
@Schema(description = "Aggregated public and internal impact figures")
public class FundImpactResponseDto {

    @Schema(description = "Total families helped with burial support", example = "15")
    private Long familiesAssisted;

    @Schema(description = "Total cumulative amount disbursed in burial support in GBP", example = "22500.00")
    private BigDecimal totalBurialSupportDisbursed;

    @Schema(description = "Average financial assistance per family in GBP", example = "1500.00")
    private BigDecimal averageAssistanceAmount;

    @Schema(description = "Number of active applications in progress", example = "4")
    private Long activeApplications;

    @Schema(description = "Currency", example = "GBP")
    @Builder.Default
    private String currency = "GBP";
}
