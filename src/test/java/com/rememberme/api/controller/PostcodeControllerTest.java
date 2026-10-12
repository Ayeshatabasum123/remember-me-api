package com.rememberme.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rememberme.api.dto.request.GpsPostcodeRequest;
import com.rememberme.api.dto.request.PostcodeValidateRequest;
import com.rememberme.api.dto.response.GpsPostcodeResponse;
import com.rememberme.api.dto.response.PostcodeValidationResponse;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtUtil;
import com.rememberme.api.service.PostcodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostcodeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PostcodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostcodeService postcodeService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void validatePostcode_ReturnsValidationResponse() throws Exception {
        PostcodeValidateRequest request = new PostcodeValidateRequest("SW1A 1AA");
        PostcodeValidationResponse response = PostcodeValidationResponse.builder()
                .valid(true)
                .postcode("SW1A 1AA")
                .country("England")
                .region("London")
                .build();

        when(postcodeService.validatePostcode("SW1A 1AA")).thenReturn(response);

        mockMvc.perform(post("/api/postcodes/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.postcode").value("SW1A 1AA"))
                .andExpect(jsonPath("$.country").value("England"))
                .andExpect(jsonPath("$.region").value("London"));
    }

    @Test
    public void fromGps_ReturnsResolvedPostcode() throws Exception {
        GpsPostcodeRequest request = new GpsPostcodeRequest(51.507, -0.127);
        GpsPostcodeResponse response = GpsPostcodeResponse.builder()
                .postcode("SW1A 1AA")
                .country("England")
                .region("London")
                .latitude(51.507)
                .longitude(-0.127)
                .build();

        when(postcodeService.lookupFromGps(51.507, -0.127)).thenReturn(response);

        mockMvc.perform(post("/api/postcodes/from-gps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.postcode").value("SW1A 1AA"))
                .andExpect(jsonPath("$.data.country").value("England"))
                .andExpect(jsonPath("$.data.region").value("London"));
    }
}
