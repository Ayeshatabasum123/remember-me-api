package com.rememberme.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GraveRequest {

    @NotNull
    private Long rememberMeId;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Grave or plot number (alphanumeric, e.g. 1, 12, A101, GR-205, Unknown)", example = "A101")
    private String graveNumber;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Postcode (optional, e.g. SW1A 1AA, NW3 2QD, EC4M 8AD)", example = "SW1A 1AA")
    private String postcode;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private Double locationAccuracy;
}
