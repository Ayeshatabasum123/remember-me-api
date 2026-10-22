package com.rememberme.api.communityfund.dto.request;

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
@Schema(description = "Admin burial provider verification payload")
public class AdminProviderVerificationDto {

    @NotNull(message = "Verified boolean flag is required")
    @Schema(description = "Whether the burial provider was successfully verified", example = "true")
    private Boolean verified;

    @Schema(description = "Verification notes documenting verification method", example = "Provider details verified.")
    private String verificationNotes;
}
