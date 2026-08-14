package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.Memorial;
import com.rememberme.api.repository.MemorialRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/{id}")
    public ApiResponse<Memorial> updateMemorial(@PathVariable Long id, @RequestBody Memorial updated) {
        Memorial memorial = memorialRepository.findById(id).orElseThrow();
        memorial.setBiography(updated.getBiography());
        memorial.setPrayerOrMessage(updated.getPrayerOrMessage());
        return ApiResponse.success(memorialRepository.save(memorial));
    }
}
