package com.rememberme.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rememberme.api.dto.response.GpsPostcodeResponse;
import com.rememberme.api.dto.response.PostcodeValidationResponse;
import com.rememberme.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostcodeService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String POSTCODES_IO_BASE_URL = "https://api.postcodes.io";

    public String normalizePostcode(String postcode) {
        if (postcode == null) {
            return null;
        }
        return postcode.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    public PostcodeValidationResponse validatePostcode(String rawPostcode) {
        if (!StringUtils.hasText(rawPostcode)) {
            throw new ApiException("Postcode must not be empty", HttpStatus.BAD_REQUEST);
        }

        String normalized = normalizePostcode(rawPostcode);
        String url = POSTCODES_IO_BASE_URL + "/postcodes/" + URLEncoder.encode(normalized, StandardCharsets.UTF_8);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                int status = root.path("status").asInt(200);
                if (status == 200 && root.has("result") && !root.path("result").isNull()) {
                    JsonNode result = root.path("result");
                    String canonicalPostcode = result.path("postcode").asText(rawPostcode);
                    String country = result.path("country").asText(null);
                    String region = result.path("region").asText(null);
                    Double latitude = result.hasNonNull("latitude") ? result.path("latitude").asDouble() : null;
                    Double longitude = result.hasNonNull("longitude") ? result.path("longitude").asDouble() : null;

                    return PostcodeValidationResponse.builder()
                            .valid(true)
                            .postcode(canonicalPostcode)
                            .normalizedPostcode(normalizePostcode(canonicalPostcode))
                            .country(country)
                            .region(region)
                            .latitude(latitude)
                            .longitude(longitude)
                            .build();
                }
            }
            return PostcodeValidationResponse.builder()
                    .valid(false)
                    .postcode(rawPostcode)
                    .normalizedPostcode(normalized)
                    .message("Invalid UK postcode")
                    .build();
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("Postcode not found on Postcodes.io: {}", normalized);
            return PostcodeValidationResponse.builder()
                    .valid(false)
                    .postcode(rawPostcode)
                    .normalizedPostcode(normalized)
                    .message("Invalid UK postcode")
                    .build();
        } catch (HttpClientErrorException ex) {
            log.warn("Postcodes.io returned client error: {} {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return PostcodeValidationResponse.builder()
                    .valid(false)
                    .postcode(rawPostcode)
                    .normalizedPostcode(normalized)
                    .message("Invalid UK postcode")
                    .build();
        } catch (ResourceAccessException | HttpServerErrorException ex) {
            log.error("External Postcodes.io service failure or timeout: {}", ex.getMessage());
            throw new ApiException("External postcode service is currently unavailable. Please try again later.", HttpStatus.BAD_GATEWAY);
        } catch (Exception ex) {
            log.error("Unexpected error validating postcode with Postcodes.io: {}", ex.getMessage(), ex);
            throw new ApiException("External postcode service error: " + ex.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    public GpsPostcodeResponse lookupFromGps(Double latitude, Double longitude) {
        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            throw new ApiException("Latitude must be between -90 and 90", HttpStatus.BAD_REQUEST);
        }
        if (longitude == null || longitude < -180.0 || longitude > 180.0) {
            throw new ApiException("Longitude must be between -180 and 180", HttpStatus.BAD_REQUEST);
        }

        String url = String.format(Locale.ROOT, "%s/postcodes?lat=%f&lon=%f", POSTCODES_IO_BASE_URL, latitude, longitude);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode result = root.path("result");
                if (result.isArray() && !result.isEmpty()) {
                    JsonNode nearest = result.get(0);
                    String canonical = nearest.path("postcode").asText();
                    String country = nearest.path("country").asText(null);
                    String region = nearest.path("region").asText(null);
                    Double resLat = nearest.hasNonNull("latitude") ? nearest.path("latitude").asDouble() : null;
                    Double resLon = nearest.hasNonNull("longitude") ? nearest.path("longitude").asDouble() : null;

                    return GpsPostcodeResponse.builder()
                            .postcode(canonical)
                            .normalizedPostcode(normalizePostcode(canonical))
                            .country(country)
                            .region(region)
                            .latitude(resLat)
                            .longitude(resLon)
                            .build();
                }
            }
            throw new ApiException("No UK postcode found for the provided coordinates", HttpStatus.NOT_FOUND);
        } catch (ApiException ex) {
            throw ex;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ApiException("No UK postcode found for the provided coordinates", HttpStatus.NOT_FOUND);
        } catch (ResourceAccessException | HttpServerErrorException ex) {
            log.error("External Postcodes.io service failure or timeout during GPS lookup: {}", ex.getMessage());
            throw new ApiException("External postcode service is currently unavailable. Please try again later.", HttpStatus.BAD_GATEWAY);
        } catch (Exception ex) {
            log.error("Unexpected error looking up postcode from GPS with Postcodes.io: {}", ex.getMessage(), ex);
            throw new ApiException("External postcode service error: " + ex.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }
}
