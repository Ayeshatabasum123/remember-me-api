package com.rememberme.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rememberme.api.entity.Event;
import com.rememberme.api.entity.EventStatus;
import com.rememberme.api.entity.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Event details response")
public class EventResponseDto {
    @Schema(description = "Event ID", example = "1")
    private Long id;

    @Schema(description = "Organization ID", example = "2")
    private Long organizationId;

    @Schema(description = "Organization name", example = "Westminster Community Church")
    private String organizationName;

    @Schema(description = "Organization postcode", example = "SW1A 1AA")
    private String organizationPostcode;

    @Schema(description = "Location of the event (postcode or venue)", example = "SW1A 1AA")
    private String location;

    @Schema(description = "Event title", example = "Funeral Announcement")
    private String title;

    @Schema(description = "Event description", example = "Funeral service at the organization chapel.")
    private String description;

    @Schema(description = "Type of event (FUNERAL, RELIGIOUS_FUNCTION, OTHER)", example = "FUNERAL")
    private EventType eventType;

    @Schema(description = "Event date and time (ISO-8601 with timezone offset)", example = "2026-10-15T14:00:00+01:00")
    private OffsetDateTime eventDateTime;

    @Schema(description = "Event date (YYYY-MM-DD)", example = "2026-10-15")
    private String date;

    @Schema(description = "Event time (HH:mm:ss)", example = "14:00:00")
    private String time;

    @Schema(description = "Event status (DRAFT, PUBLISHED)", example = "PUBLISHED")
    private EventStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private static final java.time.format.DateTimeFormatter TIME_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");

    public static EventResponseDto fromEntity(Event event) {
        if (event == null) return null;
        String orgPostcode = event.getOrganization() != null ? event.getOrganization().getPostcode() : null;
        String orgName = event.getOrganization() != null ? event.getOrganization().getName() : null;
        String loc = orgPostcode != null ? orgPostcode : orgName;

        String dateStr = event.getEventDateTime() != null ? event.getEventDateTime().toLocalDate().toString() : null;
        String timeStr = event.getEventDateTime() != null ? event.getEventDateTime().toLocalTime().format(TIME_FORMATTER) : null;

        return EventResponseDto.builder()
                .id(event.getId())
                .organizationId(event.getOrganization() != null ? event.getOrganization().getId() : null)
                .organizationName(orgName)
                .organizationPostcode(orgPostcode)
                .location(loc)
                .title(event.getTitle())
                .description(event.getDescription())
                .eventType(event.getEventType())
                .eventDateTime(event.getEventDateTime())
                .date(dateStr)
                .time(timeStr)
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
