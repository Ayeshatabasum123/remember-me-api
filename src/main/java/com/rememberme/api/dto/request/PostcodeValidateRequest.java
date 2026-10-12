package com.rememberme.api.dto.request;

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
public class PostcodeValidateRequest {

    @NotBlank(message = "Postcode is required")
    @Schema(example = "SW1A 1AA")
    private String postcode;
}
