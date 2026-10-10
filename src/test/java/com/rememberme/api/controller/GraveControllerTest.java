package com.rememberme.api.controller;

import com.rememberme.api.entity.Grave;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.DeceasedPersonRepository;
import com.rememberme.api.repository.GraveRepository;
import com.rememberme.api.repository.RememberMeRepository;
import com.rememberme.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
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
    private DeceasedPersonRepository deceasedPersonRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void searchGraves_ByCity_Found() throws Exception {
        RememberMe rememberMe = RememberMe.builder()
                .id(1L)
                .name("Delhi Memorial")
                .city("Delhi")
                .country("India")
                .build();
        Grave grave = Grave.builder()
                .id(1L)
                .graveNumber("12")
                .rememberMe(rememberMe)
                .latitude(28.6139)
                .longitude(77.2090)
                .build();

        when(graveRepository.searchByCityAndCountryUser(eq("Delhi"), isNull(), any()))
                .thenReturn(List.of(grave));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/graves/search")
                        .param("city", "Delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Records found"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].city").value("Delhi"))
                .andExpect(jsonPath("$.data[0].country").value("India"))
                .andExpect(jsonPath("$.data[0].graveNumber").value(12));
    }

    @Test
    public void searchGraves_ByCountry_Found() throws Exception {
        RememberMe rememberMe = RememberMe.builder()
                .id(2L)
                .name("India Memorial")
                .city("Mumbai")
                .country("India")
                .build();
        Grave grave = Grave.builder()
                .id(2L)
                .graveNumber("A-5")
                .rememberMe(rememberMe)
                .latitude(19.0760)
                .longitude(72.8777)
                .build();

        when(graveRepository.searchByCityAndCountryUser(isNull(), eq("India"), any()))
                .thenReturn(List.of(grave));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/graves/search")
                        .param("country", "India"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Records found"))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].city").value("Mumbai"))
                .andExpect(jsonPath("$.data[0].country").value("India"))
                .andExpect(jsonPath("$.data[0].graveNumber").value("A-5"));
    }

    @Test
    public void searchGraves_ByCityAndCountry_Found() throws Exception {
        RememberMe rememberMe = RememberMe.builder()
                .id(1L)
                .name("Delhi Memorial")
                .city("Delhi")
                .country("India")
                .build();
        Grave grave = Grave.builder()
                .id(1L)
                .graveNumber("12")
                .rememberMe(rememberMe)
                .latitude(28.6139)
                .longitude(77.2090)
                .build();

        when(graveRepository.searchByCityAndCountryUser(eq("Delhi"), eq("India"), any()))
                .thenReturn(List.of(grave));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/graves/search")
                        .param("city", "Delhi")
                        .param("country", "India"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Records found"))
                .andExpect(jsonPath("$.data[0].city").value("Delhi"))
                .andExpect(jsonPath("$.data[0].country").value("India"));
    }

    @Test
    public void searchGraves_NotFound() throws Exception {
        when(graveRepository.searchByCityAndCountryUser(eq("NonExistent"), isNull(), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/graves/search")
                        .param("city", "NonExistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("No city or country found."))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    public void searchGraves_NoParams_ReturnsNoCityOrCountryFound() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/graves/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("No city or country found."))
                .andExpect(jsonPath("$.data").isEmpty());
    }

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
