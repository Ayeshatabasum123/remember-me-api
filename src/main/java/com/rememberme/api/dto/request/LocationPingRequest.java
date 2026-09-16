package com.rememberme.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User live location update ping for proximity notifications")
public class LocationPingRequest {

    @NotNull(message = "Latitude is required")
    @Schema(description = "Current device latitude", example = "37.774929")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @Schema(description = "Current device longitude", example = "-122.419416")
    private Double longitude;
}
