package com.rememberme.api.controller;

import com.rememberme.api.dto.request.GpsPostcodeRequest;
import com.rememberme.api.dto.request.PostcodeValidateRequest;
import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.dto.response.GpsPostcodeResponse;
import com.rememberme.api.dto.response.PostcodeValidationResponse;
import com.rememberme.api.service.PostcodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/postcodes")
@RequiredArgsConstructor
@Tag(name = "Postcode Services", description = "Validate UK postcodes and resolve postcodes from GPS coordinates via Postcodes.io")
public class PostcodeController {

    private final PostcodeService postcodeService;

    @PostMapping("/validate")
    @Operation(summary = "Validate UK Postcode", description = "Validates a UK postcode with Postcodes.io and returns canonical details")
    public PostcodeValidationResponse validatePostcode(@Valid @RequestBody PostcodeValidateRequest request) {
        return postcodeService.validatePostcode(request.getPostcode());
    }

    @PostMapping("/from-gps")
    @Operation(summary = "GPS to Postcode Lookup", description = "Retrieves the nearest available UK postcode for given latitude and longitude coordinates")
    public ApiResponse<GpsPostcodeResponse> fromGps(@Valid @RequestBody GpsPostcodeRequest request) {
        GpsPostcodeResponse response = postcodeService.lookupFromGps(request.getLatitude(), request.getLongitude());
        return ApiResponse.success("Postcode resolved successfully", response);
    }
}
