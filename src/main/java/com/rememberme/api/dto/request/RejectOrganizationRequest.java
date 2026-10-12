package com.rememberme.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectOrganizationRequest {

    @Schema(example = "Missing valid religious charity registration or documentation")
    private String rejectionReason;
}
