package com.rememberme.api.communityfund.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Audit trail entry recording administrative and financial actions")
public class AuditLogResponseDto {

    @Schema(description = "Audit log ID", example = "1")
    private Long id;

    @Schema(description = "User ID who performed the action", example = "99")
    private Long actorUserId;

    @Schema(description = "Email of actor who performed the action", example = "admin@rememberme.com")
    private String actorEmail;

    @Schema(description = "Action name (e.g. APPROVAL_SUBMITTED, FUNDS_RESERVED)", example = "APPROVAL_SUBMITTED")
    private String action;

    @Schema(description = "Entity type (e.g. SUPPORT_REQUEST, FUND_RESERVATION)", example = "SUPPORT_REQUEST")
    private String entityType;

    @Schema(description = "Target entity ID", example = "1")
    private Long entityId;

    @Schema(description = "Audit action details or JSON payload", example = "Approved amount: £1500.00")
    private String details;

    @Schema(description = "Timestamp of the action", example = "2026-10-22T14:30:00")
    private LocalDateTime createdAt;
}
