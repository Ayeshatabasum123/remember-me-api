package com.rememberme.api.controller;

import com.rememberme.api.entity.Grave;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GraveController.class)
@AutoConfigureMockMvc(addFilters = false)
public class GraveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GraveRepository graveRepository;

    @MockBean
    private RememberMeRepository rememberMeRepository;

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
}
