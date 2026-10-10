package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.entity.User;
import com.rememberme.api.repository.DeceasedPersonRepository;
import com.rememberme.api.repository.RememberMeRepository;
import com.rememberme.api.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Search people or rememberMes by name, city, or country")
public class SearchController {

    private final DeceasedPersonRepository deceasedPersonRepository;
    private final RememberMeRepository rememberMeRepository;
    private final UserRepository userRepository;

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

        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        String cleanCity = (city != null && !city.trim().isEmpty()) ? city.trim() : null;
        String cleanCountry = (country != null && !country.trim().isEmpty()) ? country.trim() : null;
        String cleanType = (type != null && !type.trim().isEmpty()) ? type.trim().toLowerCase() : null;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("people", Collections.emptyList());
        data.put("rememberMes", Collections.emptyList());

        // If no search criteria provided, return HTTP 200 with "No city or country found."
        if (cleanQuery == null && cleanCity == null && cleanCountry == null) {
            return ApiResponse.success("No city or country found.", data);
        }

        Authentication currentAuth = authentication != null ? authentication :
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = currentAuth != null && currentAuth.getAuthorities() != null && currentAuth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN")
                        || a.getAuthority().equals("ADMIN")
                        || a.getAuthority().equals("SUPER_ADMIN"));

        Long userId = null;
        if (currentAuth != null && currentAuth.getName() != null) {
            User user = userRepository.findByEmail(currentAuth.getName()).orElse(null);
            if (user != null) {
                userId = user.getId();
            }
        }

        List<DeceasedPerson> people;
        List<RememberMe> rememberMes;

        if (cleanCity != null || cleanCountry != null) {
            // Explicit city/country parameters
            if (isAdmin) {
                rememberMes = rememberMeRepository.searchByCityAndCountryAdmin(cleanCity, cleanCountry);
                people = deceasedPersonRepository.searchByCityAndCountryAdmin(cleanCity, cleanCountry);
            } else {
                rememberMes = rememberMeRepository.searchByCityAndCountryUser(cleanCity, cleanCountry, userId);
                people = deceasedPersonRepository.searchByCityAndCountryUser(cleanCity, cleanCountry, userId);
            }
        } else if ("person".equals(cleanType) || "people".equals(cleanType) || "name".equals(cleanType)) {
            // Explicit person search
            people = deceasedPersonRepository.findByFullNameContainingIgnoreCase(cleanQuery);
            rememberMes = Collections.emptyList();
        } else if ("memorial".equals(cleanType) || "memorials".equals(cleanType)) {
            // Explicit memorial search
            people = Collections.emptyList();
            if (isAdmin) {
                rememberMes = rememberMeRepository.findByNameContainingIgnoreCase(cleanQuery);
            } else {
                rememberMes = rememberMeRepository.searchAllFieldsUser(cleanQuery, userId).stream()
                        .filter(r -> r.getName() != null && r.getName().toLowerCase().contains(cleanQuery.toLowerCase()))
                        .toList();
            }
        } else if ("location".equals(cleanType) || "city".equals(cleanType) || "country".equals(cleanType)) {
            // Explicit location search with query
            if (isAdmin) {
                List<RememberMe> cityMatches = rememberMeRepository.searchByCityAndCountryAdmin(cleanQuery, null);
                List<RememberMe> countryMatches = rememberMeRepository.searchByCityAndCountryAdmin(null, cleanQuery);
                rememberMes = java.util.stream.Stream.concat(cityMatches.stream(), countryMatches.stream())
                        .distinct()
                        .toList();
                List<DeceasedPerson> peopleCity = deceasedPersonRepository.searchByCityAndCountryAdmin(cleanQuery, null);
                List<DeceasedPerson> peopleCountry = deceasedPersonRepository.searchByCityAndCountryAdmin(null, cleanQuery);
                people = java.util.stream.Stream.concat(peopleCity.stream(), peopleCountry.stream())
                        .distinct()
                        .toList();
            } else {
                List<RememberMe> cityMatches = rememberMeRepository.searchByCityAndCountryUser(cleanQuery, null, userId);
                List<RememberMe> countryMatches = rememberMeRepository.searchByCityAndCountryUser(null, cleanQuery, userId);
                rememberMes = java.util.stream.Stream.concat(cityMatches.stream(), countryMatches.stream())
                        .distinct()
                        .toList();
                List<DeceasedPerson> peopleCity = deceasedPersonRepository.searchByCityAndCountryUser(cleanQuery, null, userId);
                List<DeceasedPerson> peopleCountry = deceasedPersonRepository.searchByCityAndCountryUser(null, cleanQuery, userId);
                people = java.util.stream.Stream.concat(peopleCity.stream(), peopleCountry.stream())
                        .distinct()
                        .toList();
            }
        } else {
            // Default: Search across all fields (name, city, country)
            if (isAdmin) {
                rememberMes = rememberMeRepository.searchAllFieldsAdmin(cleanQuery);
                people = deceasedPersonRepository.searchByNameOrLocationAdmin(cleanQuery);
            } else {
                rememberMes = rememberMeRepository.searchAllFieldsUser(cleanQuery, userId);
                people = deceasedPersonRepository.searchByNameOrLocationUser(cleanQuery, userId);
            }
        }

        data.put("people", people != null ? people : Collections.emptyList());
        data.put("rememberMes", rememberMes != null ? rememberMes : Collections.emptyList());

        boolean hasResults = (people != null && !people.isEmpty()) || (rememberMes != null && !rememberMes.isEmpty());

        if (hasResults) {
            return ApiResponse.success("Records found", data);
        } else {
            // When no records found
            if ("person".equals(cleanType) || "people".equals(cleanType) || "name".equals(cleanType) ||
                "memorial".equals(cleanType) || "memorials".equals(cleanType)) {
                return ApiResponse.success(data);
            }
            return ApiResponse.success("No city or country found.", data);
        }
    }
}

