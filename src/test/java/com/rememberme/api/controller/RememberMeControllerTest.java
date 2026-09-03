package com.rememberme.api.controller;

import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.GraveRepository;
import com.rememberme.api.repository.RememberMeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtUtil;

@WebMvcTest(RememberMeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class RememberMeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RememberMeRepository rememberMeRepository;

    @MockBean
    private GraveRepository graveRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void deleteGraveyard_Success() throws Exception {
        RememberMe rememberMe = new RememberMe();
        rememberMe.setId(1L);

        when(rememberMeRepository.findById(1L)).thenReturn(Optional.of(rememberMe));
        when(graveRepository.existsByRememberMeId(1L)).thenReturn(false);
        doNothing().when(rememberMeRepository).delete(rememberMe);

        mockMvc.perform(delete("/api/graveyards/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Graveyard deleted successfully"));
    }

    @Test
    public void deleteGraveyard_NotFound() throws Exception {
        when(rememberMeRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/graveyards/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Graveyard not found with ID: 1"));
    }

    @Test
    public void deleteGraveyard_Conflict_HasAssociatedGraves() throws Exception {
        RememberMe rememberMe = new RememberMe();
        rememberMe.setId(1L);

        when(rememberMeRepository.findById(1L)).thenReturn(Optional.of(rememberMe));
        when(graveRepository.existsByRememberMeId(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/graveyards/{id}", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete graveyard: It has associated graves"));
    }

    @Test
    public void addGraveyard_Success() throws Exception {
        when(rememberMeRepository.existsByNameIgnoreCaseAndLatitudeAndLongitude("Greenwood", 12.34, 56.78))
                .thenReturn(false);
        
        RememberMe saved = RememberMe.builder()
                .id(1L)
                .name("Greenwood")
                .latitude(12.34)
                .longitude(56.78)
                .build();
        when(rememberMeRepository.save(any(RememberMe.class))).thenReturn(saved);

        String json = "{\"name\":\"Greenwood\",\"latitude\":12.34,\"longitude\":56.78}";

        mockMvc.perform(post("/api/graveyards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Greenwood"));
    }

    @Test
    public void addGraveyard_Conflict() throws Exception {
        when(rememberMeRepository.existsByNameIgnoreCaseAndLatitudeAndLongitude("Greenwood", 12.34, 56.78))
                .thenReturn(true);

        String json = "{\"name\":\"Greenwood\",\"latitude\":12.34,\"longitude\":56.78}";

        mockMvc.perform(post("/api/graveyards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Graveyard already exists."));
    }
}
