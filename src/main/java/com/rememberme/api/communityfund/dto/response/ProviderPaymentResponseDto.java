package com.rememberme.api.communityfund.dto.response;

import com.rememberme.api.communityfund.enums.ProviderPaymentStatus;
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
@Schema(description = "Details of a burial provider disbursement")
public class ProviderPaymentResponseDto {

    @Schema(description = "Payment ID", example = "1")
    private Long id;

    @Schema(description = "Associated support request ID", example = "1")
    private Long supportRequestId;

    @Schema(description = "Associated fund reservation ID", example = "1")
    private Long reservationId;

    @Schema(description = "Registered burial service provider name", example = "Example Funeral Services")
    private String providerName;

    @Schema(description = "Disbursed payment amount in GBP", example = "1500.00")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "GBP")
    private String currency;

    @Schema(description = "Unique audit reference or banking tracking code", example = "TEST-PROVIDER-001")
    private String paymentReference;

    @Schema(description = "Payment status: PENDING, PROCESSING, CONFIRMED, PAID, FAILED", example = "PENDING")
    private ProviderPaymentStatus status;

    @Schema(description = "ID of administrator who initiated disbursement", example = "99")
    private Long initiatedByUserId;

    @Schema(description = "ID of administrator who confirmed settlement", example = "null")
    private Long confirmedByUserId;

    @Schema(description = "Initiation timestamp", example = "2026-10-22T13:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Settlement confirmation timestamp", example = "null")
    private LocalDateTime paidAt;
}
