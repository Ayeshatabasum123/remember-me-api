package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.Memorial;
import com.rememberme.api.repository.MemorialRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.rememberme.api.exception.ApiException;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/memorials")
@RequiredArgsConstructor
@Tag(name = "Memorial", description = "Biography, prayers/messages, photos and memories")
public class MemorialController {

    private final MemorialRepository memorialRepository;

    @PostMapping
    public ApiResponse<Memorial> createMemorial(@RequestBody Memorial memorial) {
        return ApiResponse.success(memorialRepository.save(memorial));
    }

    @GetMapping("/{id}")
    public ApiResponse<Memorial> getMemorial(@PathVariable Long id) {
        return ApiResponse.success(memorialRepository.findById(id).orElseThrow());
    }

    @GetMapping("/deceased/{deceasedPersonId}")
    @Operation(summary = "Get memorial by deceased person ID", description = "Retrieve the memorial associated with a specific deceased person ID")
    public ApiResponse<Memorial> getMemorialByDeceasedPersonId(@PathVariable Long deceasedPersonId) {
        Memorial memorial = memorialRepository.findByDeceasedPersonId(deceasedPersonId)
                .orElseThrow(() -> new ApiException("Memorial not found for deceased person ID: " + deceasedPersonId, HttpStatus.NOT_FOUND));
        return ApiResponse.success(memorial);
    }

    @PutMapping("/{id}")
    public ApiResponse<Memorial> updateMemorial(@PathVariable Long id, @RequestBody Memorial updated) {
        Memorial memorial = memorialRepository.findById(id).orElseThrow();
        memorial.setBiography(updated.getBiography());
        memorial.setPrayerOrMessage(updated.getPrayerOrMessage());
        return ApiResponse.success(memorialRepository.save(memorial));
    }
}
