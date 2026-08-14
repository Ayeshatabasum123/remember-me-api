package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.Favorite;
import com.rememberme.api.repository.FavoriteRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favourites")
@RequiredArgsConstructor
@Tag(name = "Favourite / Saved Graves", description = "Save loved ones for quick access")
public class FavouriteController {

    private final FavoriteRepository favoriteRepository;

    @PostMapping
    public ApiResponse<Favorite> addFavourite(@RequestBody Favorite favorite) {
        return ApiResponse.success(favoriteRepository.save(favorite));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<Favorite>> getByUser(@PathVariable Long userId) {
        return ApiResponse.success(favoriteRepository.findAll().stream()
                .filter(f -> f.getUser().getId().equals(userId))
                .toList());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> removeFavourite(@PathVariable Long id) {
        favoriteRepository.deleteById(id);
        return ApiResponse.success("Removed from favourites", null);
    }
}
