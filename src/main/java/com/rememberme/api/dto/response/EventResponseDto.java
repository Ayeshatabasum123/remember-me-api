package com.rememberme.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rememberme.api.entity.Event;
import com.rememberme.api.entity.EventStatus;
import com.rememberme.api.entity.EventType;
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
public class EventResponseDto {
    private Long id;
    private Long organizationId;
    private String organizationName;
    private String organizationPostcode;
    private String title;
    private String description;
    private EventType eventType;
    private OffsetDateTime eventDateTime;
    private EventStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EventResponseDto fromEntity(Event event) {
        if (event == null) return null;
        return EventResponseDto.builder()
                .id(event.getId())
                .organizationId(event.getOrganization() != null ? event.getOrganization().getId() : null)
                .organizationName(event.getOrganization() != null ? event.getOrganization().getName() : null)
                .organizationPostcode(event.getOrganization() != null ? event.getOrganization().getPostcode() : null)
                .title(event.getTitle())
                .description(event.getDescription())
                .eventType(event.getEventType())
                .eventDateTime(event.getEventDateTime())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
