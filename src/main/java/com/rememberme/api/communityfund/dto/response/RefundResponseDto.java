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
@Schema(description = "Details of a processed refund")
public class RefundResponseDto {

    @Schema(description = "Refund ID", example = "1")
    private Long id;

    @Schema(description = "Original transaction ID", example = "1")
    private Long originalTransactionId;

    @Schema(description = "Associated donation ID", example = "1")
    private Long donationId;

    @Schema(description = "Refunded amount in GBP", example = "50.00")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "GBP")
    private String currency;

    @Schema(description = "Refund reason", example = "Duplicate donation payment.")
    private String reason;

    @Schema(description = "Status of the refund (e.g. COMPLETED)", example = "COMPLETED")
    private String status;

    @Schema(description = "ID of administrator who processed refund", example = "99")
    private Long processedByUserId;

    @Schema(description = "Creation timestamp", example = "2026-10-22T14:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Completion timestamp", example = "2026-10-22T14:00:00")
    private LocalDateTime completedAt;
}
