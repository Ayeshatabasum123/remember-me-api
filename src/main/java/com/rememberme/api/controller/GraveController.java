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

import com.rememberme.api.exception.ApiException;
import org.springframework.http.HttpStatus;

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

        String trimmedGraveNumber = request.getGraveNumber() != null ? request.getGraveNumber().trim() : "";
        if (!trimmedGraveNumber.isEmpty() && graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(rememberMe.getId(), trimmedGraveNumber)) {
            throw new ApiException("Grave already exists.", HttpStatus.CONFLICT);
        }

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

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteGrave(@PathVariable Long id) {
        Grave grave = graveRepository.findById(id)
                .orElseThrow(() -> new ApiException("Grave not found with ID: " + id, HttpStatus.NOT_FOUND));
        graveRepository.delete(grave);
        return ApiResponse.success("Grave deleted successfully", null);
    }
}
