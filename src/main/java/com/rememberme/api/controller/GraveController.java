package com.rememberme.api.controller;

import com.rememberme.api.dto.request.GraveRequest;
import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.Grave;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.repository.GraveRepository;
import com.rememberme.api.repository.RememberMeRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/graves")
@RequiredArgsConstructor
@Tag(name = "Grave", description = "Add an individual grave inside a rememberMe")
public class GraveController {

    private final GraveRepository graveRepository;
    private final RememberMeRepository rememberMeRepository;

    @PostMapping
    public ApiResponse<Grave> addGrave(@Valid @RequestBody GraveRequest request) {
        RememberMe rememberMe = rememberMeRepository.findById(request.getRememberMeId()).orElseThrow();

        Grave grave = Grave.builder()
                .rememberMe(rememberMe)
                .graveNumber(request.getGraveNumber())
                .section(request.getSection())
                .row(request.getRow())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationAccuracy(request.getLocationAccuracy())
                .verificationStatus(Grave.VerificationStatus.UNVERIFIED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return ApiResponse.success(graveRepository.save(grave));
    }

    @GetMapping("/rememberMe/{rememberMeId}")
    public ApiResponse<List<Grave>> listGravesByGraveyard(@PathVariable Long rememberMeId) {
        return ApiResponse.success(graveRepository.findAll().stream()
                .filter(g -> g.getRememberMe().getId().equals(rememberMeId))
                .toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<Grave> getGrave(@PathVariable Long id) {
        return ApiResponse.success(graveRepository.findById(id).orElseThrow());
    }
}
