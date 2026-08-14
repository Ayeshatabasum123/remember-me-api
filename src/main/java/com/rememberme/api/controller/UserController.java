package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.User;
import com.rememberme.api.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Stores user profile and account details")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/{id}")
    public ApiResponse<User> getUserById(@PathVariable Long id) {
        return ApiResponse.success(userRepository.findById(id).orElseThrow());
    }

    @PutMapping("/{id}")
    public ApiResponse<User> updateUser(@PathVariable Long id, @RequestBody User updated) {
        User user = userRepository.findById(id).orElseThrow();
        user.setFullName(updated.getFullName());
        user.setPhone(updated.getPhone());
        user.setPhotoUrl(updated.getPhotoUrl());
        user.setCountry(updated.getCountry());
        return ApiResponse.success("Profile updated", userRepository.save(user));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ApiResponse.success("User deleted", null);
    }
}
