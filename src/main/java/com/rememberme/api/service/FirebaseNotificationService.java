package com.rememberme.api.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.rememberme.api.config.FirebaseConfig;
import com.rememberme.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseNotificationService {

    private final FirebaseMessaging firebaseMessaging;

    @Value("${firebase.project-id:}")
    private String projectId;

    public SendResult sendToToken(String fcmToken, String title, String body) {
        return sendToToken(fcmToken, title, body, null);
    }

    public SendResult sendToToken(String fcmToken, String title, String body, java.util.Map<String, String> data) {
        if (FirebaseConfig.isUnconfigured()) {
            String reason = FirebaseConfig.getUnconfiguredReason();
            log.error("Rejecting FCM notification request because Firebase is unconfigured: {}", reason);
            throw new ApiException(reason, HttpStatus.BAD_GATEWAY);
        }

        if (FirebaseConfig.isMockMode()) {
            String mockMessageId = String.format("projects/%s/messages/mock-%d", getProjectId(), System.currentTimeMillis());
            log.info("[DEV MOCK MODE] Simulated FCM notification delivery to token: {}. Message ID: {}", fcmToken, mockMessageId);
            return SendResult.sent(mockMessageId);
        }

        Message.Builder messageBuilder = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build());

        if (data != null && !data.isEmpty()) {
            messageBuilder.putAllData(data);
        }

        Message message = messageBuilder.build();

        try {
            String messageId = firebaseMessaging.send(message);
            log.info("Successfully sent notification via Firebase Admin SDK. Message ID: {}", messageId);
            return SendResult.sent(messageId);
        } catch (FirebaseMessagingException ex) {
            MessagingErrorCode errorCode = ex.getMessagingErrorCode();
            if (errorCode == MessagingErrorCode.UNREGISTERED
                    || errorCode == MessagingErrorCode.INVALID_ARGUMENT
                    || errorCode == MessagingErrorCode.THIRD_PARTY_AUTH_ERROR
                    || (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("senderid mismatch"))) {
                log.warn("Firebase rejected an invalid, mismatched, or unregistered FCM token [{}]: {}", errorCode, ex.getMessage());
                return SendResult.invalidTokenResult();
            }

            log.error("Firebase failed to send an FCM notification: [ErrorCode: {}] {}", errorCode, ex.getMessage(), ex);
            String messageDetails = org.springframework.util.StringUtils.hasText(ex.getMessage())
                    ? "Firebase failed to send notification: " + ex.getMessage()
                    : "Firebase failed to send notification";
            throw new ApiException(messageDetails, HttpStatus.BAD_GATEWAY);
        } catch (Exception ex) {
            log.error("Unexpected error during FCM notification delivery: {}", ex.getMessage(), ex);
            String messageDetails = org.springframework.util.StringUtils.hasText(ex.getMessage())
                    ? "Firebase notification failed: " + ex.getMessage()
                    : "Firebase notification failed";
            throw new ApiException(messageDetails, HttpStatus.BAD_GATEWAY);
        }
    }

    private String getProjectId() {
        try {
            if (!com.google.firebase.FirebaseApp.getApps().isEmpty()) {
                String appProjectId = com.google.firebase.FirebaseApp.getInstance().getOptions().getProjectId();
                if (org.springframework.util.StringUtils.hasText(appProjectId)) {
                    return appProjectId;
                }
            }
        } catch (Exception ignored) {
        }
        return org.springframework.util.StringUtils.hasText(projectId) ? projectId : "remember-me-7a323";
    }

    public record SendResult(boolean sent, boolean invalidToken, String messageId) {
        public static SendResult sent(String messageId) {
            return new SendResult(true, false, messageId);
        }

        public static SendResult invalidTokenResult() {
            return new SendResult(false, true, null);
        }
    }
}
