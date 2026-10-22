package com.rememberme.api.communityfund.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Fund reservation release request payload")
public class ReleaseReservationDto {

    @Schema(description = "Audit justification for releasing reservation", example = "Application cancelled by family or duplicate reservation.")
    private String reason;
}
