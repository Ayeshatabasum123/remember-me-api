package com.rememberme.api.controller;

import com.rememberme.api.dto.request.RegisterDeviceTokenRequest;
import com.rememberme.api.dto.request.UserPostcodeRequest;
import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.dto.response.NotificationResponseDto;
import com.rememberme.api.dto.response.PostcodeValidationResponse;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.service.NotificationManagementService;
import com.rememberme.api.service.PostcodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Stores user profile, postcode settings, device tokens, and notifications")
public class UserController {

    private final UserRepository userRepository;
    private final PostcodeService postcodeService;
    private final NotificationManagementService notificationManagementService;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Authenticated user not found", HttpStatus.UNAUTHORIZED));
    }

    private void validateUserOwnership(Long targetUserId) {
        User currentUser = getAuthenticatedUser();
        if (!currentUser.getId().equals(targetUserId)) {
            throw new ApiException("Access Denied: You can only access your own profile", HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get Current User Profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<User> getMe() {
        return ApiResponse.success(getAuthenticatedUser());
    }

    @PutMapping("/me/postcode")
    @Operation(summary = "Save or Update User Postcode", description = "Validates and saves the authenticated user's UK postcode", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<User> updateMyPostcode(@Valid @RequestBody UserPostcodeRequest request) {
        User currentUser = getAuthenticatedUser();

        PostcodeValidationResponse validation = postcodeService.validatePostcode(request.getPostcode());
        if (!validation.isValid()) {
            throw new ApiException("Invalid UK postcode: " + request.getPostcode(), HttpStatus.BAD_REQUEST);
        }

        currentUser.setPostcode(validation.getPostcode());
        currentUser.setPostcodeNormalized(postcodeService.normalizePostcode(validation.getPostcode()));
        currentUser.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(currentUser);

        return ApiResponse.success("Postcode updated successfully", saved);
    }

    @PostMapping("/me/device-token")
    @Operation(summary = "Register FCM Device Token", description = "Registers or updates a Firebase device token for the authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Void> registerDeviceToken(@Valid @RequestBody RegisterDeviceTokenRequest request) {
        User currentUser = getAuthenticatedUser();
        notificationManagementService.registerDeviceToken(currentUser, request.getDeviceToken());
        return ApiResponse.success("Device token registered successfully", null);
    }

    @GetMapping("/me/notifications")
    @Operation(summary = "Retrieve User Notifications", description = "Returns notifications for the authenticated user with pagination and newest first", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Page<NotificationResponseDto>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User currentUser = getAuthenticatedUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationResponseDto> notifications = notificationManagementService.getUserNotifications(currentUser, pageable);
        return ApiResponse.success("Notifications retrieved successfully", notifications);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get User Profile by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<User> getUserById(@PathVariable Long id) {
        validateUserOwnership(id);
        return ApiResponse.success(userRepository.findById(id).orElseThrow());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update User Profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<User> updateUser(@PathVariable Long id, @RequestBody User updated) {
        validateUserOwnership(id);
        User user = userRepository.findById(id).orElseThrow();
        user.setFullName(updated.getFullName());
        user.setPhone(updated.getPhone());
        user.setPhotoUrl(updated.getPhotoUrl());
        user.setCountry(updated.getCountry());
        user.setUpdatedAt(LocalDateTime.now());
        return ApiResponse.success("Profile updated", userRepository.save(user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete User Profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        validateUserOwnership(id);
        userRepository.deleteById(id);
        return ApiResponse.success("User deleted", null);
    }
}
