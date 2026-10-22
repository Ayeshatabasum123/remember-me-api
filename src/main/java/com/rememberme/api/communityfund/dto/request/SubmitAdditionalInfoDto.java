package com.rememberme.api.communityfund.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for submitting requested additional information and supporting evidence")
public class SubmitAdditionalInfoDto {

    @NotBlank(message = "Message is required when submitting additional information")
    @Schema(description = "Confidential explanation or response to the information requested", example = "Please find the additional information requested.")
    private String message;

    @Schema(description = "IDs of uploaded supporting documents referenced in this response", example = "[3, 4]")
    private List<Long> documentIds;
}
