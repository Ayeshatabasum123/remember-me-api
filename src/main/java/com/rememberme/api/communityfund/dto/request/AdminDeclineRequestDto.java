package com.rememberme.api.communityfund.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin decline support request payload")
public class AdminDeclineRequestDto {

    @NotBlank(message = "Reason for declining the application is required")
    @Schema(description = "Formal administrative reason for declining assistance", example = "Required eligibility criteria were not met.")
    private String reason;
}
