package com.rememberme.api.communityfund.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Refund request submission payload")
public class SubmitRefundDto {

    @NotNull(message = "Original transaction ID is required")
    @Schema(description = "ID of original donation or transaction being refunded", example = "1")
    private Long originalTransactionId;

    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than zero")
    @Schema(description = "Refund amount in GBP", example = "50.00")
    private BigDecimal amount;

    @NotBlank(message = "Refund reason is required")
    @Schema(description = "Formal reason for the refund", example = "Duplicate donation payment.")
    private String reason;
}
