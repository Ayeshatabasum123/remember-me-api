package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Tag(name = "Root", description = "Root API status and health check")
public class RootController {

    @GetMapping({"/", "/api"})
    @Operation(summary = "Root API status", description = "Returns a message indicating that Remember Me API is running")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRootStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("service", "Remember Me API");
        return ResponseEntity.ok(ApiResponse.success("Remember Me API is running", data));
    }
}
