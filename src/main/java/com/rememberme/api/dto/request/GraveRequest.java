package com.rememberme.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GraveRequest {

    @NotNull
    private Long rememberMeId;

    private String graveNumber;
    private String section;
    private String row;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private Double locationAccuracy;
}
