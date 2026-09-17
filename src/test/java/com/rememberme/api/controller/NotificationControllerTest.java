package com.rememberme.api.controller;

import com.rememberme.api.config.SecurityConfig;
import com.rememberme.api.entity.User;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtAuthFilter;
import com.rememberme.api.security.JwtUtil;
import com.rememberme.api.service.FirebaseNotificationService;
import com.rememberme.api.service.FuneralNotificationEngineService;
import com.rememberme.api.service.NotificationPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private FirebaseNotificationService firebaseNotificationService;

    @MockBean
    private NotificationPreferenceService preferenceService;

    @MockBean
    private FuneralNotificationEngineService notificationEngineService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("password")
                .role(User.Role.USER)
                .enabled(true)
                .build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(currentUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void registerToken_AuthenticatedUserCanSaveToken() throws Exception {
        mockMvc.perform(post("/api/notifications/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fcmToken\":\"first-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("FCM token registered successfully"));

        assertEquals("first-token", currentUser.getFcmToken());
        verify(userRepository).save(currentUser);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void registerToken_SameUserCanUpdateToken() throws Exception {
        currentUser.setFcmToken("old-token");

        mockMvc.perform(post("/api/notifications/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fcmToken\":\"new-token\"}"))
                .andExpect(status().isOk());

        assertEquals("new-token", currentUser.getFcmToken());
        verify(userRepository).save(currentUser);
    }

    @Test
    void registerToken_UnauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(post("/api/notifications/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fcmToken\":\"token\"}"))
                .andExpect(status().isForbidden());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void sendTestNotification_RetrievesSavedToken() throws Exception {
        currentUser.setFcmToken("saved-token");
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(firebaseNotificationService.sendToToken(
                eq("saved-token"), any(String.class), any(String.class), any()))
                .thenReturn(FirebaseNotificationService.SendResult.sent("message-id"));

        mockMvc.perform(post("/api/notifications/test").queryParam("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("message-id"));

        verify(firebaseNotificationService).sendToToken(
                eq("saved-token"), any(String.class), any(String.class), any());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void sendTestNotification_MissingTokenReturnsClearResponse() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        mockMvc.perform(post("/api/notifications/test").queryParam("userId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("FCM token not found for user"));

        verify(firebaseNotificationService, never()).sendToToken(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void sendTestNotification_RejectsAnotherUserId() throws Exception {
        mockMvc.perform(post("/api/notifications/test").queryParam("userId", "2"))
                .andExpect(status().isForbidden());

        verify(userRepository, never()).findById(2L);
        verify(firebaseNotificationService, never()).sendToToken(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void sendTestNotification_ClearsInvalidToken() throws Exception {
        currentUser.setFcmToken("invalid-token");
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(firebaseNotificationService.sendToToken(
                eq("invalid-token"), any(String.class), any(String.class), any()))
                .thenReturn(FirebaseNotificationService.SendResult.invalidTokenResult());

        mockMvc.perform(post("/api/notifications/test").queryParam("userId", "1"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").value("FCM token is invalid or no longer registered"));

        assertNull(currentUser.getFcmToken());
        verify(userRepository).save(currentUser);
    }
}
