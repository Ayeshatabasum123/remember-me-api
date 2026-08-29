package com.rememberme.api.controller;

import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.Memorial;
import com.rememberme.api.repository.MemorialRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemorialController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MemorialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemorialRepository memorialRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void getMemorialByDeceasedPersonId_Success() throws Exception {
        DeceasedPerson deceased = new DeceasedPerson();
        deceased.setId(10L);

        Memorial memorial = Memorial.builder()
                .id(1L)
                .deceasedPerson(deceased)
                .biography("Loving memory")
                .prayerOrMessage("Rest in peace")
                .build();

        when(memorialRepository.findByDeceasedPersonId(10L)).thenReturn(Optional.of(memorial));

        mockMvc.perform(get("/api/memorials/deceased/{deceasedPersonId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.biography").value("Loving memory"))
                .andExpect(jsonPath("$.data.prayerOrMessage").value("Rest in peace"));
    }

    @Test
    public void getMemorialByDeceasedPersonId_NotFound() throws Exception {
        when(memorialRepository.findByDeceasedPersonId(10L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/memorials/deceased/{deceasedPersonId}", 10L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Memorial not found for deceased person ID: 10"));
    }

    @Test
    public void getMemorialByDeceasedPersonId_InvalidFormat() throws Exception {
        mockMvc.perform(get("/api/memorials/deceased/{deceasedPersonId}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Parameter 'deceasedPersonId' is of invalid format/type"));
    }
}
