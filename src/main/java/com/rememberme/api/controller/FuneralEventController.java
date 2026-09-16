package com.rememberme.api.controller;

import com.rememberme.api.dto.request.FuneralEventRequest;
import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.FuneralEvent;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.DeceasedPersonRepository;
import com.rememberme.api.repository.FuneralEventRepository;
import com.rememberme.api.repository.RememberMeRepository;
import com.rememberme.api.service.FuneralNotificationEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/funerals")
@Tag(name = "Funerals", description = "Funeral events and push notification scheduling")
@RequiredArgsConstructor
public class FuneralEventController {

    private final FuneralEventRepository funeralEventRepository;
    private final DeceasedPersonRepository deceasedPersonRepository;
    private final RememberMeRepository rememberMeRepository;
    private final FuneralNotificationEngineService notificationEngineService;

    @PostMapping
    @Operation(summary = "Schedule a funeral event and trigger nearby push notifications", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<FuneralEvent> createFuneralEvent(@Valid @RequestBody FuneralEventRequest request) {
        DeceasedPerson deceased = deceasedPersonRepository.findById(request.getDeceasedPersonId())
                .orElseThrow(() -> new ApiException("Deceased person not found", HttpStatus.NOT_FOUND));

        RememberMe graveyard = null;
        if (request.getGraveyardId() != null) {
            graveyard = rememberMeRepository.findById(request.getGraveyardId())
                    .orElseThrow(() -> new ApiException("Graveyard not found", HttpStatus.NOT_FOUND));
        }

        FuneralEvent funeral = FuneralEvent.builder()
                .deceasedPerson(deceased)
                .graveyard(graveyard)
                .religion(request.getReligion())
                .funeralDateTime(request.getFuneralDateTime())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isVip(request.isVip())
                .isNational(request.isNational())
                .targetCountry(request.getTargetCountry())
                .targetRegion(request.getTargetRegion())
                .title(request.getTitle() != null ? request.getTitle() : "Funeral Service for " + deceased.getFullName())
                .description(request.getDescription())
                .status(FuneralEvent.FuneralStatus.SCHEDULED)
                .build();

        FuneralEvent saved = funeralEventRepository.save(funeral);

        // Dispatch nearby notifications to users matching distance and religion filter
        notificationEngineService.dispatchNearbyFuneralNotifications(saved);

        return ApiResponse.success("Funeral event created and notifications dispatched", saved);
    }

    @GetMapping
    @Operation(summary = "List upcoming scheduled funerals")
    public ApiResponse<List<FuneralEvent>> getUpcomingFunerals() {
        List<FuneralEvent> upcoming = funeralEventRepository.findUpcomingFunerals(LocalDateTime.now().minusHours(2));
        return ApiResponse.success("Upcoming funerals retrieved", upcoming);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get funeral event by ID")
    public ApiResponse<FuneralEvent> getFuneralById(@PathVariable Long id) {
        FuneralEvent funeral = funeralEventRepository.findById(id)
                .orElseThrow(() -> new ApiException("Funeral event not found", HttpStatus.NOT_FOUND));
        return ApiResponse.success("Funeral event retrieved", funeral);
    }
}
