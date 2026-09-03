package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.dto.response.PaginatedResponse;
import com.rememberme.api.dto.response.RecentMemorialDto;
import com.rememberme.api.entity.Memorial;
import com.rememberme.api.repository.MemorialRepository;
import com.rememberme.api.service.MemorialService;
import com.rememberme.api.dto.response.RecentMemorialsResponseDto;
import com.rememberme.api.exception.InvalidPaginationException;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import com.rememberme.api.exception.ApiException;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequiredArgsConstructor
@Tag(name = "Memorial", description = "Biography, prayers/messages, photos and memories")
public class MemorialController {

    private final MemorialRepository memorialRepository;
    private final MemorialService memorialService;

    @PostMapping("/api/memorials")
    public ApiResponse<Memorial> createMemorial(@RequestBody Memorial memorial) {
        return ApiResponse.success(memorialRepository.save(memorial));
    }

    @GetMapping("/api/memorials/{id}")
    public ApiResponse<Memorial> getMemorial(@PathVariable Long id) {
        return ApiResponse.success(memorialRepository.findById(id).orElseThrow());
    }

    @GetMapping("/api/memorials/deceased/{deceasedPersonId}")
    @Operation(summary = "Get memorial by deceased person ID", description = "Retrieve the memorial associated with a specific deceased person ID")
    public ApiResponse<Memorial> getMemorialByDeceasedPersonId(@PathVariable Long deceasedPersonId) {
        Memorial memorial = memorialRepository.findByDeceasedPersonId(deceasedPersonId)
                .orElseThrow(() -> new ApiException("Memorial not found for deceased person ID: " + deceasedPersonId, HttpStatus.NOT_FOUND));
        return ApiResponse.success(memorial);
    }

    @PutMapping("/api/memorials/{id}")
    public ApiResponse<Memorial> updateMemorial(@PathVariable Long id, @RequestBody Memorial updated) {
        Memorial memorial = memorialRepository.findById(id).orElseThrow();
        memorial.setBiography(updated.getBiography());
        memorial.setPrayerOrMessage(updated.getPrayerOrMessage());
        return ApiResponse.success(memorialRepository.save(memorial));
    }

    @GetMapping({"/api/v1/memorials/recent/summary", "/api/memorials/recent"})
    @Operation(summary = "Get recent memorials/tributes summary", description = "Retrieve latest memorial records sorted by creation date descending with pagination")
    public ApiResponse<RecentMemorialsResponseDto> getRecentMemorialSummaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        if (page < 0 || limit < 1 || limit > 50) {
            throw new InvalidPaginationException("Page index must be non-negative and limit must be between 1 and 50");
        }
        PageRequest pageRequest = PageRequest.of(page, limit, Sort.by("createdAt").descending());
        return ApiResponse.success(memorialService.getRecentMemorialSummaries(pageRequest));
    }

    @DeleteMapping("/api/memorials/{id}")
    @Operation(summary = "Delete memorial", description = "Delete a memorial by ID")
    public ApiResponse<Void> deleteMemorial(@PathVariable Long id) {
        memorialService.deleteMemorial(id);
        return ApiResponse.success("Memorial deleted successfully", null);
    }
}

