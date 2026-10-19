package com.rememberme.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dedication details linked to a donation")
public class DedicationResponse {

    @Schema(description = "Dedication record ID", example = "1")
    private Long id;

    @Schema(description = "Name of the loved one being remembered", example = "Fatima Khan")
    private String dedicationName;

    @Schema(description = "Optional ID of a deceased person profile", example = "42")
    private Long deceasedPersonId;

    @Schema(description = "Optional ID of a memorial profile", example = "15")
    private Long memorialId;

    @Schema(description = "Optional tribute or prayer message", example = "In loving memory and eternal prayers.")
    private String message;
}
