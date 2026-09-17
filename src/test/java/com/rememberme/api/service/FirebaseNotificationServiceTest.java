package com.rememberme.api.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.rememberme.api.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirebaseNotificationServiceTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    private FirebaseNotificationService service;

    @BeforeEach
    void setUp() {
        service = new FirebaseNotificationService(firebaseMessaging);
    }

    @Test
    void sendToToken_SuccessfulDeliveryReturnsMessageId() throws Exception {
        when(firebaseMessaging.send(any(Message.class))).thenReturn("msg-12345");

        FirebaseNotificationService.SendResult result = service.sendToToken(
                "valid-token", "Title", "Body");

        assertTrue(result.sent());
        assertFalse(result.invalidToken());
        assertEquals("msg-12345", result.messageId());
    }

    @Test
    void sendToToken_UnregisteredTokenReturnsInvalidTokenResult() throws Exception {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(firebaseMessaging.send(any(Message.class))).thenThrow(exception);

        FirebaseNotificationService.SendResult result = service.sendToToken(
                "unregistered-token", "Title", "Body");

        assertFalse(result.sent());
        assertTrue(result.invalidToken());
        assertNull(result.messageId());
    }

    @Test
    void sendToToken_WithDataPayload_SuccessfulDeliveryReturnsMessageId() throws Exception {
        when(firebaseMessaging.send(any(Message.class))).thenReturn("projects/remember-me-eb236/messages/0:17123456789");

        FirebaseNotificationService.SendResult result = service.sendToToken(
                "valid-token", "Test notification", "Push notification test", java.util.Map.of("type", "test"));

        assertTrue(result.sent());
        assertFalse(result.invalidToken());
        assertEquals("projects/remember-me-eb236/messages/0:17123456789", result.messageId());
        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    void sendToToken_GenericExceptionThrowsBadGatewayExceptionWithoutMockFallback() throws Exception {
        when(firebaseMessaging.send(any(Message.class))).thenThrow(new RuntimeException("Connection timeout to FCM server"));

        ApiException ex = assertThrows(ApiException.class, () ->
                service.sendToToken("token", "Title", "Body"));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        assertTrue(ex.getMessage().contains("Firebase notification failed"));
    }
}
