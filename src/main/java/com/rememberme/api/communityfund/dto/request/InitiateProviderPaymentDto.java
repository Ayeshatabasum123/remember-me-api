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
@Schema(description = "Burial provider payment initiation payload")
public class InitiateProviderPaymentDto {

    @NotNull(message = "Support request ID is required")
    @Schema(description = "Associated approved support request ID", example = "1")
    private Long supportRequestId;

    @NotNull(message = "Reservation ID is required")
    @Schema(description = "Associated active fund reservation ID", example = "1")
    private Long reservationId;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    @Schema(description = "Disbursement amount in GBP", example = "1500.00")
    private BigDecimal amount;

    @Schema(description = "Payment currency (default: GBP)", example = "GBP")
    private String currency;

    @NotBlank(message = "Payment reference is required")
    @Schema(description = "Unique audit reference or banking tracking code", example = "TEST-PROVIDER-001")
    private String paymentReference;
}
