package com.rememberme.api.dto.request;

import com.rememberme.api.entity.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventRequest {

    @NotBlank(message = "Title is required")
    @Schema(example = "Updated Funeral Announcement")
    private String title;

    @Schema(example = "Updated service details and schedule.")
    private String description;

    @NotNull(message = "Event type is required (FUNERAL, RELIGIOUS_FUNCTION, OTHER)")
    @Schema(example = "FUNERAL")
    private EventType eventType;

    @NotNull(message = "Event date and time is required")
    @Schema(example = "2026-10-15T15:00:00+01:00")
    private OffsetDateTime eventDateTime;
}
