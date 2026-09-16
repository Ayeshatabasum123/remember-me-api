package com.rememberme.api.dto.request;

import com.rememberme.api.entity.Religion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User notification settings and category preferences")
public class NotificationPreferencesDto {

    @NotNull(message = "nearbyFuneralsEnabled is required")
    @Schema(description = "Enable notifications for nearby funerals", example = "true")
    private Boolean nearbyFuneralsEnabled;

    @NotNull(message = "nearbyRadiusKm is required")
    @Positive(message = "Radius must be positive")
    @Schema(description = "Notification radius for nearby funerals in kilometers", example = "10.0")
    private Double nearbyRadiusKm;

    @NotNull(message = "religions set is required")
    @Schema(description = "Filter nearby funerals by religion (or ALL for any religion)", example = "[\"ISLAM\", \"CHRISTIANITY\"]")
    private Set<Religion> religions;

    @NotNull(message = "famousGravesEnabled is required")
    @Schema(description = "Enable proximity notifications when traveling near famous or historical burial places", example = "true")
    private Boolean famousGravesEnabled;

    @NotNull(message = "vipFuneralsEnabled is required")
    @Schema(description = "Enable broadcast notifications for VIP or national funerals", example = "true")
    private Boolean vipFuneralsEnabled;
}
