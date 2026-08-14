package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Push/email/SMS notifications (Firebase Cloud Messaging)")
public class NotificationController {

    @PostMapping("/test")
    public ApiResponse<String> sendTestNotification(@RequestParam String userId) {
        // TODO: integrate Firebase Cloud Messaging here
        return ApiResponse.success("Test notification sent", "OK");
    }
}
