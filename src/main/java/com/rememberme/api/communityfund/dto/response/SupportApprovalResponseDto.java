package com.rememberme.api.communityfund.dto.response;

import com.rememberme.api.communityfund.enums.ApprovalDecision;
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
@Schema(description = "Record of an administrative approval decision")
public class SupportApprovalResponseDto {

    @Schema(description = "Approval record ID", example = "1")
    private Long id;

    @Schema(description = "Support request ID", example = "1")
    private Long supportRequestId;

    @Schema(description = "Approver user ID", example = "99")
    private Long approverUserId;

    @Schema(description = "Approver full name", example = "Jane Doe")
    private String approverName;

    @Schema(description = "Approver email", example = "admin1@rememberme.com")
    private String approverEmail;

    @Schema(description = "Approval decision: APPROVED or DECLINED", example = "APPROVED")
    private ApprovalDecision decision;

    @Schema(description = "Approved amount in GBP", example = "1500.00")
    private BigDecimal approvedAmount;

    @Schema(description = "Decision justification", example = "Eligible verified burial expenses.")
    private String decisionReason;

    @Schema(description = "Decision timestamp", example = "2026-10-22T12:00:00")
    private LocalDateTime createdAt;
}
