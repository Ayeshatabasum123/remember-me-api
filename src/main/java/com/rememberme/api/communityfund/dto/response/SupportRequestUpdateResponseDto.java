package com.rememberme.api.communityfund.dto.response;

import com.rememberme.api.communityfund.enums.SupportRequestStatus;
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
@Schema(description = "Chronological application status update entry")
public class SupportRequestUpdateResponseDto {

    @Schema(description = "Update record ID", example = "5")
    private Long id;

    @Schema(description = "Support request ID", example = "1")
    private Long supportRequestId;

    @Schema(description = "Previous status before this update", example = "PENDING")
    private SupportRequestStatus previousStatus;

    @Schema(description = "New status following this update", example = "UNDER_REVIEW")
    private SupportRequestStatus newStatus;

    @Schema(description = "Discreet update message or guidance", example = "Your application is currently under administrative review.")
    private String updateMessage;

    @Schema(description = "Timestamp of the status update", example = "2026-10-22T11:00:00")
    private LocalDateTime createdAt;
}
