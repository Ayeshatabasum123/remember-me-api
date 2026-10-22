package com.rememberme.api.communityfund.dto.response;

import com.rememberme.api.communityfund.enums.SupportRequestStatus;
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
@Schema(description = "Confirmation response returned upon support request creation")
public class SupportRequestCreateResponseDto {

    @Schema(description = "Unique support application identifier", example = "1")
    private Long id;

    @Schema(description = "Initial status of the application", example = "PENDING")
    private SupportRequestStatus status;

    @Schema(description = "Approved or requested assistance amount in GBP", example = "2000.00")
    private BigDecimal requestedAmount;

    @Schema(description = "Currency code", example = "GBP")
    private String currency;
}
