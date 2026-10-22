package com.rememberme.api.communityfund.dto.request;

import com.rememberme.api.communityfund.enums.SupportRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin review update payload")
public class AdminReviewRequestDto {

    @NotNull(message = "Status is required")
    @Schema(description = "Updated status for the application (e.g. UNDER_REVIEW)", example = "UNDER_REVIEW")
    private SupportRequestStatus status;

    @Schema(description = "Internal administrative review notes", example = "Applicant details verified.")
    private String reviewNotes;
}
