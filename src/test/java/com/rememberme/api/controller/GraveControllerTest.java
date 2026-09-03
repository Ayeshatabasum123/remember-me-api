package com.rememberme.api.controller;

import com.rememberme.api.entity.Grave;
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

@WebMvcTest(GraveController.class)
@AutoConfigureMockMvc(addFilters = false)
public class GraveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GraveRepository graveRepository;

    @MockBean
    private RememberMeRepository rememberMeRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void deleteGrave_Success() throws Exception {
        Grave grave = new Grave();
        grave.setId(1L);

        when(graveRepository.findById(1L)).thenReturn(Optional.of(grave));
        doNothing().when(graveRepository).delete(grave);

        mockMvc.perform(delete("/api/graves/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Grave deleted successfully"));
    }

    @Test
    public void deleteGrave_NotFound() throws Exception {
        when(graveRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/graves/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Grave not found with ID: 1"));
    }

    @Test
    public void addGrave_Success() throws Exception {
        RememberMe rememberMe = new RememberMe();
        rememberMe.setId(1L);
        when(rememberMeRepository.findById(1L)).thenReturn(Optional.of(rememberMe));
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "A-12")).thenReturn(false);

        Grave saved = Grave.builder()
                .id(10L)
                .rememberMe(rememberMe)
                .graveNumber("A-12")
                .latitude(12.34)
                .longitude(56.78)
                .build();
        when(graveRepository.save(any(Grave.class))).thenReturn(saved);

        String json = "{\"rememberMeId\":1,\"graveNumber\":\"A-12\",\"latitude\":12.34,\"longitude\":56.78}";

        mockMvc.perform(post("/api/graves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.graveNumber").value("A-12"));
    }

    @Test
    public void addGrave_Conflict() throws Exception {
        RememberMe rememberMe = new RememberMe();
        rememberMe.setId(1L);
        when(rememberMeRepository.findById(1L)).thenReturn(Optional.of(rememberMe));
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "A-12")).thenReturn(true);

        String json = "{\"rememberMeId\":1,\"graveNumber\":\"A-12\",\"latitude\":12.34,\"longitude\":56.78}";

        mockMvc.perform(post("/api/graves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Grave already exists."));
    }
}
