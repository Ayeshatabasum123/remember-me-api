package com.rememberme.api.communityfund.dto.response;

import com.rememberme.api.communityfund.enums.SupportRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Confidential application status lookup response")
public class SupportRequestStatusResponseDto {

    @Schema(description = "Support request ID", example = "1")
    private Long requestId;

    @Schema(description = "Current lifecycle status", example = "MORE_INFO_REQUIRED")
    private SupportRequestStatus status;

    @Schema(description = "Contextual confidential status message", example = "Additional invoice details are required.")
    private String message;
}
