package com.rememberme.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rememberme.api.dto.response.GpsPostcodeResponse;
import com.rememberme.api.dto.response.PostcodeValidationResponse;
import com.rememberme.api.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostcodeServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private PostcodeService postcodeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        postcodeService = new PostcodeService(restTemplate, objectMapper);
    }

    @Test
    public void normalizePostcode_HandlesSpacesAndCase() {
        assertEquals("SW1A1AA", postcodeService.normalizePostcode("sw1a 1aa"));
        assertEquals("SW1A1AA", postcodeService.normalizePostcode("SW1A 1AA"));
        assertEquals("SW1A1AA", postcodeService.normalizePostcode("  sw1a   1aa  "));
        assertEquals("E16AN", postcodeService.normalizePostcode("e1 6an"));
        assertEquals("E16AN", postcodeService.normalizePostcode("E16AN"));
        assertNull(postcodeService.normalizePostcode(null));
    }

    @Test
    public void validatePostcode_Success() {
        String mockJsonResponse = """
            {
                "status": 200,
                "result": {
                    "postcode": "SW1A 1AA",
                    "country": "England",
                    "region": "London",
                    "latitude": 51.501009,
                    "longitude": -0.141588
                }
            }
            """;

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockJsonResponse, HttpStatus.OK));

        PostcodeValidationResponse response = postcodeService.validatePostcode("sw1a 1aa");

        assertTrue(response.isValid());
        assertEquals("SW1A 1AA", response.getPostcode());
        assertEquals("SW1A1AA", response.getNormalizedPostcode());
        assertEquals("England", response.getCountry());
        assertEquals("London", response.getRegion());
    }

    @Test
    public void validatePostcode_NotFound_ReturnsInvalid() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(org.springframework.web.client.HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND, "Not Found", org.springframework.http.HttpHeaders.EMPTY, new byte[0], java.nio.charset.StandardCharsets.UTF_8));

        PostcodeValidationResponse response = postcodeService.validatePostcode("INVALID99");

        assertFalse(response.isValid());
        assertEquals("INVALID99", response.getPostcode());
    }

    @Test
    public void validatePostcode_Empty_ThrowsBadRequest() {
        ApiException ex = assertThrows(ApiException.class, () -> postcodeService.validatePostcode("   "));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    public void validatePostcode_ServiceUnavailable_ThrowsBadGateway() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        ApiException ex = assertThrows(ApiException.class, () -> postcodeService.validatePostcode("SW1A 1AA"));
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
    }

    @Test
    public void lookupFromGps_Success() {
        String mockJsonResponse = """
            {
                "status": 200,
                "result": [
                    {
                        "postcode": "SW1A 1AA",
                        "country": "England",
                        "region": "London",
                        "latitude": 51.501009,
                        "longitude": -0.141588
                    }
                ]
            }
            """;

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockJsonResponse, HttpStatus.OK));

        GpsPostcodeResponse response = postcodeService.lookupFromGps(51.507, -0.127);

        assertNotNull(response);
        assertEquals("SW1A 1AA", response.getPostcode());
        assertEquals("SW1A1AA", response.getNormalizedPostcode());
        assertEquals("England", response.getCountry());
        assertEquals("London", response.getRegion());
    }

    @Test
    public void lookupFromGps_InvalidCoordinates_ThrowsBadRequest() {
        ApiException ex1 = assertThrows(ApiException.class, () -> postcodeService.lookupFromGps(95.0, 0.0));
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatus());

        ApiException ex2 = assertThrows(ApiException.class, () -> postcodeService.lookupFromGps(0.0, -190.0));
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatus());
    }

    @Test
    public void lookupFromGps_NoPostcodeFound_ThrowsNotFound() {
        String mockJsonResponse = """
            {
                "status": 200,
                "result": []
            }
            """;

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockJsonResponse, HttpStatus.OK));

        ApiException ex = assertThrows(ApiException.class, () -> postcodeService.lookupFromGps(51.507, -0.127));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
