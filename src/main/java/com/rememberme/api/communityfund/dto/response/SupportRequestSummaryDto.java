package com.rememberme.api.communityfund.dto.response;

import com.rememberme.api.communityfund.enums.SupportRequestStatus;
import com.rememberme.api.communityfund.enums.SupportUrgency;
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
@Schema(description = "Summary representation of a support request for applicant tracking list")
public class SupportRequestSummaryDto {

    @Schema(description = "Application ID", example = "1")
    private Long id;

    @Schema(description = "Requested assistance amount", example = "2000.00")
    private BigDecimal requestedAmount;

    @Schema(description = "Currency", example = "GBP")
    private String currency;

    @Schema(description = "Current lifecycle status", example = "UNDER_REVIEW")
    private SupportRequestStatus status;

    @Schema(description = "Urgency level", example = "HIGH")
    private SupportUrgency urgency;

    @Schema(description = "Application submission timestamp", example = "2026-10-22T10:00:00")
    private LocalDateTime createdAt;
}
