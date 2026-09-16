package com.rememberme.api.dto.request;

import com.rememberme.api.entity.Religion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body to schedule a funeral event and trigger notifications")
public class FuneralEventRequest {

    @NotNull(message = "Deceased person ID is required")
    private Long deceasedPersonId;

    private Long graveyardId;

    @NotNull(message = "Religion is required")
    private Religion religion;

    @NotNull(message = "Funeral date and time is required")
    private LocalDateTime funeralDateTime;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    private boolean isVip;
    private boolean isNational;

    private String targetCountry;
    private String targetRegion;

    private String title;
    private String description;
}
