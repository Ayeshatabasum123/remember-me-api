package com.rememberme.api.service;

import com.rememberme.api.dto.response.NotificationResponseDto;
import com.rememberme.api.entity.DeviceToken;
import com.rememberme.api.entity.Event;
import com.rememberme.api.entity.Notification;
import com.rememberme.api.entity.User;
import com.rememberme.api.repository.DeviceTokenRepository;
import com.rememberme.api.repository.NotificationRepository;
import com.rememberme.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationManagementService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PostcodeService postcodeService;
    private final FirebaseNotificationService firebaseNotificationService;

    @Transactional
    public void registerDeviceToken(User user, String tokenValue) {
        if (!StringUtils.hasText(tokenValue)) {
            return;
        }

        // Clean up from other users if device changed hands
        Optional<DeviceToken> existingOtherUser = deviceTokenRepository.findByDeviceToken(tokenValue);
        if (existingOtherUser.isPresent() && !existingOtherUser.get().getUser().getId().equals(user.getId())) {
            deviceTokenRepository.delete(existingOtherUser.get());
        }

        // Check if current user already has this token
        Optional<DeviceToken> existingUserToken = deviceTokenRepository.findByUserIdAndDeviceToken(user.getId(), tokenValue);
        if (existingUserToken.isEmpty()) {
            DeviceToken deviceToken = DeviceToken.builder()
                    .user(user)
                    .deviceToken(tokenValue)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            deviceTokenRepository.save(deviceToken);
            log.info("Registered new device token for user ID {}", user.getId());
        } else {
            DeviceToken token = existingUserToken.get();
            token.setUpdatedAt(LocalDateTime.now());
            deviceTokenRepository.save(token);
        }

        // Also sync user.fcmToken for backward compatibility
        user.setFcmToken(tokenValue);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public int processEventNotifications(Event event) {
        if (event == null || event.getOrganization() == null) {
            log.warn("Cannot process notifications for null event or null organization");
            return 0;
        }

        String rawOrgPostcode = event.getOrganization().getPostcode();
        String normalizedOrgPostcode = postcodeService.normalizePostcode(rawOrgPostcode);

        if (!StringUtils.hasText(normalizedOrgPostcode)) {
            log.warn("Organization ID {} has invalid/empty postcode", event.getOrganization().getId());
            return 0;
        }

        List<User> matchingUsers = userRepository.findByPostcodeNormalized(normalizedOrgPostcode);
        if (matchingUsers.isEmpty()) {
            log.info("No registered users match normalized postcode '{}' for published event ID {}",
                    normalizedOrgPostcode, event.getId());
            return 0;
        }

        log.info("Found {} registered users with normalized postcode '{}' for published event ID {}",
                matchingUsers.size(), normalizedOrgPostcode, event.getId());

        int createdCount = 0;
        String title = event.getTitle();
        String body = StringUtils.hasText(event.getDescription())
                ? event.getDescription()
                : (event.getOrganization().getName() + " announced a " + event.getEventType() + " event.");

        Map<String, String> dataPayload = new HashMap<>();
        dataPayload.put("type", "EVENT_NOTIFICATION");
        dataPayload.put("eventId", String.valueOf(event.getId()));
        dataPayload.put("organizationId", String.valueOf(event.getOrganization().getId()));
        dataPayload.put("organizationName", event.getOrganization().getName());
        dataPayload.put("eventType", event.getEventType().name());

        for (User user : matchingUsers) {
            // Deduplication: prevent duplicate notification records for the same published event and user
            if (notificationRepository.existsByEventIdAndUserId(event.getId(), user.getId())) {
                log.info("Notification for event ID {} and user ID {} already exists. Skipping duplicate.",
                        event.getId(), user.getId());
                continue;
            }

            // Push notifications via FCM
            boolean anySent = sendPushToUserDevices(user, title, body, dataPayload);

            Notification notification = Notification.builder()
                    .event(event)
                    .user(user)
                    .title(title)
                    .message(body)
                    .isRead(false)
                    .deliveryStatus(anySent ? "SENT" : "FAILED")
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            createdCount++;
        }

        return createdCount;
    }

    private boolean sendPushToUserDevices(User user, String title, String body, Map<String, String> data) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(user.getId());
        boolean anySent = false;

        if (!tokens.isEmpty()) {
            for (DeviceToken dt : tokens) {
                try {
                    FirebaseNotificationService.SendResult result = firebaseNotificationService.sendToToken(
                            dt.getDeviceToken(), title, body, data
                    );
                    if (result.invalidToken()) {
                        log.warn("FCM token {} is invalid or unregistered. Removing.", dt.getDeviceToken());
                        deviceTokenRepository.delete(dt);
                    } else if (result.sent()) {
                        anySent = true;
                    }
                } catch (Exception ex) {
                    log.error("Failed to deliver push notification to token {}: {}", dt.getDeviceToken(), ex.getMessage());
                }
            }
        } else if (StringUtils.hasText(user.getFcmToken())) {
            try {
                FirebaseNotificationService.SendResult result = firebaseNotificationService.sendToToken(
                        user.getFcmToken(), title, body, data
                );
                if (result.invalidToken()) {
                    user.setFcmToken(null);
                    userRepository.save(user);
                } else if (result.sent()) {
                    anySent = true;
                }
            } catch (Exception ex) {
                log.error("Failed to deliver push notification to user fcmToken: {}", ex.getMessage());
            }
        }

        return anySent;
    }

    public Page<NotificationResponseDto> getUserNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(NotificationResponseDto::fromEntity);
    }
}
