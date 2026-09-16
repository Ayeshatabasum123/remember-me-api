package com.rememberme.api.controller;

import com.rememberme.api.dto.request.FcmTokenRequest;
import com.rememberme.api.dto.request.LocationPingRequest;
import com.rememberme.api.dto.request.NotificationPreferencesDto;
import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.dto.response.LocationPingResponse;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.service.FirebaseNotificationService;
import com.rememberme.api.service.FuneralNotificationEngineService;
import com.rememberme.api.service.NotificationPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Push notifications, preferences, and location proximity services")
@RequiredArgsConstructor
public class NotificationController {

    private final UserRepository userRepository;
    private final FirebaseNotificationService firebaseNotificationService;
    private final NotificationPreferenceService preferenceService;
    private final FuneralNotificationEngineService notificationEngineService;

    @PostMapping("/token")
    @Operation(
            summary = "Register or update the logged-in user's FCM token",
            description = "Uses the bearer JWT identity; no user ID is accepted from the client.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "FCM token saved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "FCM token is missing or blank",
                    content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Bearer JWT is missing or invalid")
    })
    public ApiResponse<Void> registerToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "The current Firebase Cloud Messaging registration token")
            @Valid @RequestBody FcmTokenRequest request,
            Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        user.setFcmToken(request.getFcmToken());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return ApiResponse.success("FCM token registered successfully", null);
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get current user's notification category preferences", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<NotificationPreferencesDto> getPreferences(Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        NotificationPreferencesDto dto = preferenceService.getPreferences(user);
        return ApiResponse.success("Notification preferences retrieved", dto);
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update current user's notification category preferences", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<NotificationPreferencesDto> updatePreferences(
            @Valid @RequestBody NotificationPreferencesDto dto,
            Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        NotificationPreferencesDto updated = preferenceService.updatePreferences(user, dto);
        return ApiResponse.success("Notification preferences updated successfully", updated);
    }

    @PostMapping("/location-ping")
    @Operation(
            summary = "Update device location ping and check for nearby famous graves & active funerals",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<LocationPingResponse> processLocationPing(
            @Valid @RequestBody LocationPingRequest request,
            Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        LocationPingResponse response = notificationEngineService.processLocationPing(
                user, request.getLatitude(), request.getLongitude());
        return ApiResponse.success("Location ping processed successfully", response);
    }

    @PostMapping("/test")
    @Operation(
            summary = "Send a test push notification",
            description = "Sends to the saved FCM token of the authenticated user. The userId must identify that same user.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Firebase accepted the message"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "The user has no registered FCM token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The userId does not belong to the authenticated user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "Firebase rejected an invalid or unregistered token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Firebase could not send the notification")
    })
    public ApiResponse<String> sendTestNotification(
            @Parameter(in = ParameterIn.QUERY, required = true, description = "ID of the authenticated user")
            @RequestParam("userId") Long userId,
            Authentication authentication) {
        User authenticatedUser = findAuthenticatedUser(authentication);
        if (!authenticatedUser.getId().equals(userId)) {
            throw new ApiException("Access Denied: You can only send a test notification to yourself", HttpStatus.FORBIDDEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        if (!StringUtils.hasText(user.getFcmToken())) {
            throw new ApiException("FCM token is not registered for this user", HttpStatus.BAD_REQUEST);
        }

        FirebaseNotificationService.SendResult result = firebaseNotificationService.sendToToken(
                user.getFcmToken(),
                "Remember Me test notification",
                "Firebase Cloud Messaging is configured successfully.");

        if (result.invalidToken()) {
            user.setFcmToken(null);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            throw new ApiException("FCM token is invalid or no longer registered", HttpStatus.GONE);
        }

        return ApiResponse.success("Test notification sent", result.messageId());
    }

    private User findAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException("Authentication is required", HttpStatus.UNAUTHORIZED);
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException("Authenticated user not found", HttpStatus.UNAUTHORIZED));
    }
}
