package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Stores user profile and account details")
public class UserController {

    private final UserRepository userRepository;

    private void validateUserOwnership(Long targetUserId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Authenticated user not found", HttpStatus.UNAUTHORIZED));
        
        if (!currentUser.getId().equals(targetUserId)) {
            throw new ApiException("Access Denied: You can only access your own profile", HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/me")
    public ApiResponse<User> getMe() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return ApiResponse.success(currentUser);
    }

    @GetMapping("/{id}")
    public ApiResponse<User> getUserById(@PathVariable Long id) {
        validateUserOwnership(id);
        return ApiResponse.success(userRepository.findById(id).orElseThrow());
    }

    @PutMapping("/{id}")
    public ApiResponse<User> updateUser(@PathVariable Long id, @RequestBody User updated) {
        validateUserOwnership(id);
        User user = userRepository.findById(id).orElseThrow();
        user.setFullName(updated.getFullName());
        user.setPhone(updated.getPhone());
        user.setPhotoUrl(updated.getPhotoUrl());
        user.setCountry(updated.getCountry());
        return ApiResponse.success("Profile updated", userRepository.save(user));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        validateUserOwnership(id);
        userRepository.deleteById(id);
        return ApiResponse.success("User deleted", null);
    }
}
