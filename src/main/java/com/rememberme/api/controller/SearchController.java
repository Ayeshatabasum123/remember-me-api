package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Search people or rememberMes by name, city, or country")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(
            summary = "Search people and memorials by query, city, or country",
            description = "Searches for matching people and rememberMes by query, city, or country with case-insensitive partial matching."
    )
    public ApiResponse<Map<String, Object>> search(
            @Parameter(description = "Search query for person name, memorial name, city, or country")
            @RequestParam(value = "query", required = false) String query,
            @Parameter(description = "City name to search (case-insensitive, partial matching)")
            @RequestParam(value = "city", required = false) String city,
            @Parameter(description = "Country name to search (case-insensitive, partial matching)")
            @RequestParam(value = "country", required = false) String country,
            @Parameter(description = "Filter type (e.g. location, person, memorial)")
            @RequestParam(value = "type", required = false) String type,
            Authentication authentication) {

        return searchService.search(query, city, country, type, authentication);
    }
}


