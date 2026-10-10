package com.rememberme.api.controller;

import com.rememberme.api.dto.request.GraveRequest;
import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.dto.response.GraveResponseDto;
import com.rememberme.api.dto.response.PaginatedResponse;
import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.Grave;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.DeceasedPersonRepository;
import com.rememberme.api.repository.GraveRepository;
import com.rememberme.api.repository.RememberMeRepository;
import com.rememberme.api.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/graves")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Grave", description = "Add, list, and search individual graves inside a graveyard/rememberMe")
public class GraveController {

    private final GraveRepository graveRepository;
    private final RememberMeRepository rememberMeRepository;
    private final DeceasedPersonRepository deceasedPersonRepository;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Add an individual grave inside a graveyard", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Grave> addGrave(@Valid @RequestBody GraveRequest request) {
        RememberMe rememberMe = rememberMeRepository.findById(request.getRememberMeId()).orElseThrow();

        String trimmedGraveNumber = request.getGraveNumber() != null ? request.getGraveNumber().trim() : "";
        if (!trimmedGraveNumber.isEmpty() && graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(rememberMe.getId(), trimmedGraveNumber)) {
            throw new ApiException("Grave already exists.", HttpStatus.CONFLICT);
        }

        Grave grave = Grave.builder()
                .rememberMe(rememberMe)
                .graveNumber(request.getGraveNumber())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationAccuracy(request.getLocationAccuracy())
                .verificationStatus(Grave.VerificationStatus.UNVERIFIED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return ApiResponse.success(graveRepository.save(grave));
    }

    /**
     * Dynamic City and Country Search API for both Users and Admins.
     * Case-insensitive partial matching on city and country.
     * Accessible by USER, ADMIN, and SUPER_ADMIN roles without pagination.
     */
    @GetMapping("/search")
    @Operation(
            summary = "Dynamic City and Country Search for Graves",
            description = "Searches for graves by city, country, or both with case-insensitive partial matching. " +
                    "Both Users and Admins are authorized. Admins can view all matching graves, while Users can view " +
                    "matching graves in approved graveyards. Returns 200 with 'No city or country found.' when no records match.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<List<GraveResponseDto>> searchGraves(
            @Parameter(description = "City name to search (case-insensitive, partial matching)", example = "Delhi")
            @RequestParam(value = "city", required = false) String city,
            @Parameter(description = "Country name to search (case-insensitive, partial matching)", example = "India")
            @RequestParam(value = "country", required = false) String country,
            Authentication authentication) {

        String cleanCity = (city != null && !city.trim().isEmpty()) ? city.trim() : null;
        String cleanCountry = (country != null && !country.trim().isEmpty()) ? country.trim() : null;

        if (cleanCity == null && cleanCountry == null) {
            return ApiResponse.success("No city or country found.", Collections.emptyList());
        }

        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN")
                        || a.getAuthority().equals("ADMIN")
                        || a.getAuthority().equals("SUPER_ADMIN"));

        List<Grave> matchingGraves;
        if (isAdmin) {
            matchingGraves = graveRepository.searchByCityAndCountryAdmin(cleanCity, cleanCountry);
        } else {
            Long userId = null;
            if (authentication != null && authentication.getName() != null) {
                User user = userRepository.findByEmail(authentication.getName()).orElse(null);
                if (user != null) {
                    userId = user.getId();
                }
            }
            matchingGraves = graveRepository.searchByCityAndCountryUser(cleanCity, cleanCountry, userId);
        }

        if (matchingGraves == null || matchingGraves.isEmpty()) {
            return ApiResponse.success("No city or country found.", Collections.emptyList());
        }

        List<GraveResponseDto> dtos = matchingGraves.stream()
                .map(this::mapToDto)
                .toList();

        return ApiResponse.success("Records found", dtos);
    }

    /**
     * General Grave List API supporting optional graveyard filter and pagination.
     */
    @GetMapping
    @Operation(summary = "List graves with optional filtering and pagination", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<?> listGraves(
            @RequestParam(required = false) Long graveyardId,
            @RequestParam(required = false) Long rememberMeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Long targetGraveyardId = graveyardId != null ? graveyardId : rememberMeId;
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() :
                ((query != null && !query.trim().isEmpty()) ? query.trim() : null);

        if (page != null || size != null) {
            int pageNum = page != null && page >= 0 ? page : 0;
            int pageSize = size != null && size > 0 ? size : 20;
            Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "id"));
            Page<Grave> gravePage;
            if (targetGraveyardId != null || searchTerm != null) {
                gravePage = graveRepository.searchGraves(targetGraveyardId, searchTerm, pageable);
            } else {
                gravePage = graveRepository.findAll(pageable);
            }
            List<GraveResponseDto> dtos = gravePage.getContent().stream()
                    .map(this::mapToDto)
                    .toList();
            PaginatedResponse<GraveResponseDto> response = PaginatedResponse.<GraveResponseDto>builder()
                    .content(dtos)
                    .pagination(PaginatedResponse.PaginationMetadata.builder()
                            .page(gravePage.getNumber())
                            .size(gravePage.getSize())
                            .totalElements(gravePage.getTotalElements())
                            .totalPages(gravePage.getTotalPages())
                            .hasNext(gravePage.hasNext())
                            .build())
                    .build();
            return ApiResponse.success(response);
        }

        List<Grave> graves;
        if (targetGraveyardId != null) {
            graves = graveRepository.findByRememberMeId(targetGraveyardId);
        } else {
            graves = graveRepository.findAll();
        }
        if (searchTerm != null) {
            String lowerSearch = searchTerm.toLowerCase();
            graves = graves.stream()
                    .filter(g -> (g.getGraveNumber() != null && g.getGraveNumber().toLowerCase().contains(lowerSearch))
                            || (g.getRememberMe() != null && g.getRememberMe().getName() != null && g.getRememberMe().getName().toLowerCase().contains(lowerSearch)))
                    .toList();
        }
        List<GraveResponseDto> dtos = graves.stream().map(this::mapToDto).toList();
        return ApiResponse.success(dtos);
    }

    @GetMapping("/rememberMe/{rememberMeId}")
    @Operation(summary = "List graves inside a specific graveyard", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<List<GraveResponseDto>> listGravesByGraveyard(@PathVariable Long rememberMeId) {
        List<GraveResponseDto> dtos = graveRepository.findByRememberMeId(rememberMeId).stream()
                .map(this::mapToDto)
                .toList();
        return ApiResponse.success(dtos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single grave by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<GraveResponseDto> getGrave(@PathVariable Long id) {
        Grave grave = graveRepository.findById(id).orElseThrow();
        return ApiResponse.success(mapToDto(grave));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a grave by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Void> deleteGrave(@PathVariable Long id) {
        Grave grave = graveRepository.findById(id)
                .orElseThrow(() -> new ApiException("Grave not found with ID: " + id, HttpStatus.NOT_FOUND));
        graveRepository.delete(grave);
        return ApiResponse.success("Grave deleted successfully", null);
    }

    private GraveResponseDto mapToDto(Grave grave) {
        if (grave == null) {
            return null;
        }

        RememberMe graveyard = grave.getRememberMe();
        DeceasedPerson deceased = deceasedPersonRepository.findFirstByGraveId(grave.getId()).orElse(null);

        return GraveResponseDto.builder()
                .id(grave.getId())
                .city(graveyard != null ? graveyard.getCity() : null)
                .country(graveyard != null ? graveyard.getCountry() : null)
                .graveNumber(GraveResponseDto.parseGraveNumber(grave.getGraveNumber()))
                .latitude(grave.getLatitude())
                .longitude(grave.getLongitude())
                .locationAccuracy(grave.getLocationAccuracy())
                .verificationStatus(grave.getVerificationStatus())
                .graveyardId(graveyard != null ? graveyard.getId() : null)
                .rememberMeId(graveyard != null ? graveyard.getId() : null)
                .graveyardName(graveyard != null ? graveyard.getName() : null)
                .cemeteryName(graveyard != null ? graveyard.getName() : null)
                .deceasedPersonId(deceased != null ? deceased.getId() : null)
                .deceasedName(deceased != null ? deceased.getFullName() : null)
                .dateOfBirth(deceased != null ? deceased.getDateOfBirth() : null)
                .dateOfDeath(deceased != null ? deceased.getDateOfDeath() : null)
                .biography(deceased != null ? deceased.getBiography() : null)
                .gender(deceased != null ? deceased.getGender() : null)
                .photoUrl(deceased != null ? deceased.getPhotoUrl() : null)
                .createdAt(grave.getCreatedAt())
                .updatedAt(grave.getUpdatedAt())
                .build();
    }
}
