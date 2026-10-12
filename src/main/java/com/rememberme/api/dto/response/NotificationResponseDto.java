package com.rememberme.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rememberme.api.entity.EventType;
import com.rememberme.api.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponseDto {
    private Long notificationId;
    private Long eventId;
    private String organizationName;
    private String eventTitle;
    private String eventDescription;
    private EventType eventType;
    private LocalDateTime createdDate;
    private boolean readStatus;
    private String deliveryStatus;

    public static NotificationResponseDto fromEntity(Notification notification) {
        if (notification == null) return null;
        return NotificationResponseDto.builder()
                .notificationId(notification.getId())
                .eventId(notification.getEvent() != null ? notification.getEvent().getId() : null)
                .organizationName(notification.getEvent() != null && notification.getEvent().getOrganization() != null
                        ? notification.getEvent().getOrganization().getName() : null)
                .eventTitle(notification.getEvent() != null ? notification.getEvent().getTitle() : notification.getTitle())
                .eventDescription(notification.getEvent() != null ? notification.getEvent().getDescription() : notification.getMessage())
                .eventType(notification.getEvent() != null ? notification.getEvent().getEventType() : null)
                .createdDate(notification.getCreatedAt())
                .readStatus(notification.isRead())
                .deliveryStatus(notification.getDeliveryStatus())
                .build();
    }
}
