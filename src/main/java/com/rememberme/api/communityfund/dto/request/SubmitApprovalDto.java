package com.rememberme.api.communityfund.dto.request;

import com.rememberme.api.communityfund.enums.ApprovalDecision;
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
@Schema(description = "Two-person administrative approval decision submission")
public class SubmitApprovalDto {

    @NotNull(message = "Decision is required (APPROVED or DECLINED)")
    @Schema(description = "Approval decision: APPROVED or DECLINED", example = "APPROVED")
    private ApprovalDecision decision;

    @NotNull(message = "Approved amount is required")
    @DecimalMin(value = "0.01", message = "Approved amount must be greater than zero")
    @Schema(description = "Authorized assistance amount in GBP", example = "1500.00")
    private BigDecimal approvedAmount;

    @NotBlank(message = "Reason for approval decision is required")
    @Schema(description = "Justification explaining why assistance was approved or declined", example = "Eligible verified burial expenses.")
    private String reason;
}
