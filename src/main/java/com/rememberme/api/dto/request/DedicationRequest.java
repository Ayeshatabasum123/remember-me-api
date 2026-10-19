package com.rememberme.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Optional dedication honoring a loved one. Dedications do not allocate funds to individuals.")
public class DedicationRequest {

    @NotBlank(message = "Dedication name is required")
    @Schema(description = "Name of the loved one being remembered", example = "Fatima Khan")
    private String dedicationName;

    @Schema(description = "Optional ID of a deceased person profile in the app", example = "42")
    private Long deceasedPersonId;

    @Schema(description = "Optional ID of a memorial profile in the app", example = "15")
    private Long memorialId;

    @Schema(description = "Optional tribute, prayer, or remembrance note", example = "In loving memory and eternal prayers.")
    private String message;
}
