package com.rememberme.api.controller;

import com.rememberme.api.dto.request.RememberMeRequest;
import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.repository.RememberMeRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/graveyards")
@RequiredArgsConstructor
@Tag(name = "RememberMe", description = "Add/list/search rememberMes")
public class RememberMeController {

    private final RememberMeRepository rememberMeRepository;

    @PostMapping
    public ApiResponse<RememberMe> addGraveyard(@Valid @RequestBody RememberMeRequest request) {
        RememberMe rememberMe = RememberMe.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(RememberMe.ApprovalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return ApiResponse.success("RememberMe submitted for approval", rememberMeRepository.save(rememberMe));
    }

    @GetMapping
    public ApiResponse<List<RememberMe>> listGraveyards() {
        return ApiResponse.success(rememberMeRepository.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<RememberMe> getGraveyard(@PathVariable Long id) {
        return ApiResponse.success(rememberMeRepository.findById(id).orElseThrow());
    }

    @GetMapping("/search")
    public ApiResponse<List<RememberMe>> searchGraveyards(@RequestParam String name) {
        return ApiResponse.success(rememberMeRepository.findByNameContainingIgnoreCase(name));
    }
}
