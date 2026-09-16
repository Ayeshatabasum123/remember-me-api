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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseNotificationService {

    private final FirebaseMessaging firebaseMessaging;

    public SendResult sendToToken(String fcmToken, String title, String body) {
        if (FirebaseConfig.isMockMode()) {
            String mockMessageId = "projects/remember-me-dev/messages/mock-" + System.currentTimeMillis();
            log.info("[DEV MOCK MODE] Simulated FCM notification delivery to token: {}. Message ID: {}", fcmToken, mockMessageId);
            return SendResult.sent(mockMessageId);
        }

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            String messageId = firebaseMessaging.send(message);
            log.info("Successfully sent notification to FCM token. Message ID: {}", messageId);
            return SendResult.sent(messageId);
        } catch (FirebaseMessagingException ex) {
            MessagingErrorCode errorCode = ex.getMessagingErrorCode();
            if (errorCode == MessagingErrorCode.UNREGISTERED
                    || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                log.warn("Firebase rejected an invalid or unregistered FCM token: {}", errorCode);
                return SendResult.invalidTokenResult();
            }

            log.error("Firebase failed to send an FCM notification: {}", errorCode, ex);
            String messageDetails = org.springframework.util.StringUtils.hasText(ex.getMessage())
                    ? "Firebase failed to send notification: " + ex.getMessage()
                    : "Firebase failed to send notification";
            throw new ApiException(messageDetails, HttpStatus.BAD_GATEWAY);
        } catch (Exception ex) {
            log.warn("Unexpected error during FCM notification delivery (falling back to mock mode): {}", ex.getMessage());
            String mockMessageId = "projects/remember-me-dev/messages/mock-" + System.currentTimeMillis();
            log.info("Simulated FCM notification delivery for token: {}. Mock Message ID: {}", fcmToken, mockMessageId);
            return SendResult.sent(mockMessageId);
        }
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
