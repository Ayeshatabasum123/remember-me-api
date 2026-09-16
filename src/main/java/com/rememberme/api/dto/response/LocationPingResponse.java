package com.rememberme.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response returned after processing a user location ping")
public class LocationPingResponse {

    @Schema(description = "Count of active nearby funerals within user's configured radius", example = "2")
    private int nearbyFuneralsCount;

    @Schema(description = "Famous or historical graveyards near the user's current location")
    private List<FamousGraveyardSummaryDto> famousGravesNearby;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FamousGraveyardSummaryDto {
        private Long id;
        private String name;
        private String city;
        private String country;
        private Double distanceKm;
        private String historicalDetails;
    }
}
