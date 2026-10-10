package com.rememberme.api.controller;

import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.entity.User;
import com.rememberme.api.repository.DeceasedPersonRepository;
import com.rememberme.api.repository.RememberMeRepository;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeceasedPersonRepository deceasedPersonRepository;

    @MockBean
    private RememberMeRepository rememberMeRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Search by city found returns Records found and matching records")
    public void search_ByCity_Found() throws Exception {
        RememberMe memorial = RememberMe.builder()
                .id(10L)
                .name("Raj Ghat")
                .city("New Delhi")
                .country("India")
                .status(RememberMe.ApprovalStatus.APPROVED)
                .build();

        when(rememberMeRepository.searchAllFieldsUser(eq("New Delhi"), any()))
                .thenReturn(List.of(memorial));
        when(deceasedPersonRepository.searchByNameOrLocationUser(eq("New Delhi"), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/search")
                        .param("query", "New Delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Records found"))
                .andExpect(jsonPath("$.data.rememberMes[0].id").value(10))
                .andExpect(jsonPath("$.data.rememberMes[0].name").value("Raj Ghat"))
                .andExpect(jsonPath("$.data.rememberMes[0].city").value("New Delhi"))
                .andExpect(jsonPath("$.data.rememberMes[0].country").value("India"))
                .andExpect(jsonPath("$.data.people").isArray())
                .andExpect(jsonPath("$.data.people").isEmpty());
    }

    @Test
    @DisplayName("Search by country found returns Records found and matching records")
    public void search_ByCountry_Found() throws Exception {
        RememberMe memorial = RememberMe.builder()
                .id(10L)
                .name("Raj Ghat")
                .city("New Delhi")
                .country("India")
                .status(RememberMe.ApprovalStatus.APPROVED)
                .build();

        when(rememberMeRepository.searchAllFieldsUser(eq("India"), any()))
                .thenReturn(List.of(memorial));
        when(deceasedPersonRepository.searchByNameOrLocationUser(eq("India"), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/search")
                        .param("query", "India"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Records found"))
                .andExpect(jsonPath("$.data.rememberMes[0].id").value(10))
                .andExpect(jsonPath("$.data.rememberMes[0].country").value("India"));
    }

    @Test
    @DisplayName("Search by non-existent city or country returns 200 with 'No city or country found.'")
    public void search_NotFound_ReturnsNoCityOrCountryFound() throws Exception {
        when(rememberMeRepository.searchAllFieldsUser(eq("Atlantis"), any()))
                .thenReturn(Collections.emptyList());
        when(deceasedPersonRepository.searchByNameOrLocationUser(eq("Atlantis"), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/search")
                        .param("query", "Atlantis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("No city or country found."))
                .andExpect(jsonPath("$.data.people").isArray())
                .andExpect(jsonPath("$.data.people").isEmpty())
                .andExpect(jsonPath("$.data.rememberMes").isArray())
                .andExpect(jsonPath("$.data.rememberMes").isEmpty());
    }

    @Test
    @DisplayName("Search with no parameters returns 200 with 'No city or country found.'")
    public void search_NoParams_ReturnsNoCityOrCountryFound() throws Exception {
        mockMvc.perform(get("/api/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("No city or country found."))
                .andExpect(jsonPath("$.data.people").isArray())
                .andExpect(jsonPath("$.data.people").isEmpty())
                .andExpect(jsonPath("$.data.rememberMes").isArray())
                .andExpect(jsonPath("$.data.rememberMes").isEmpty());
    }

    @Test
    @DisplayName("Search by memorial name found preserves memorial search functionality")
    public void search_ByMemorialName_Found() throws Exception {
        RememberMe memorial = RememberMe.builder()
                .id(10L)
                .name("Raj Ghat")
                .city("New Delhi")
                .country("India")
                .status(RememberMe.ApprovalStatus.APPROVED)
                .build();

        when(rememberMeRepository.searchAllFieldsUser(eq("Raj Ghat"), any()))
                .thenReturn(List.of(memorial));
        when(deceasedPersonRepository.searchByNameOrLocationUser(eq("Raj Ghat"), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/search")
                        .param("query", "Raj Ghat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Records found"))
                .andExpect(jsonPath("$.data.rememberMes[0].name").value("Raj Ghat"));
    }

    @Test
    @DisplayName("Search by person name found preserves person search functionality")
    public void search_ByPersonName_Found() throws Exception {
        DeceasedPerson person = DeceasedPerson.builder()
                .id(1L)
                .fullName("Mahatma Gandhi")
                .build();

        when(rememberMeRepository.searchAllFieldsUser(eq("Mahatma Gandhi"), any()))
                .thenReturn(Collections.emptyList());
        when(deceasedPersonRepository.searchByNameOrLocationUser(eq("Mahatma Gandhi"), any()))
                .thenReturn(List.of(person));

        mockMvc.perform(get("/api/search")
                        .param("query", "Mahatma Gandhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Records found"))
                .andExpect(jsonPath("$.data.people[0].fullName").value("Mahatma Gandhi"));
    }

    @Test
    @DisplayName("Search with ADMIN role uses admin queries")
    @WithMockUser(roles = "ADMIN")
    public void search_AdminRole_UsesAdminQueries() throws Exception {
        RememberMe memorial = RememberMe.builder()
                .id(10L)
                .name("Pending Memorial")
                .city("New Delhi")
                .country("India")
                .status(RememberMe.ApprovalStatus.PENDING)
                .build();

        when(rememberMeRepository.searchAllFieldsAdmin(eq("Delhi")))
                .thenReturn(List.of(memorial));
        when(deceasedPersonRepository.searchByNameOrLocationAdmin(eq("Delhi")))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/search")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .param("query", "Delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Records found"))
                .andExpect(jsonPath("$.data.rememberMes[0].id").value(10));
    }

    @Test
    @DisplayName("Explicit city and country parameters work and return matching records")
    public void search_ByExplicitCityAndCountry_Found() throws Exception {
        RememberMe memorial = RememberMe.builder()
                .id(10L)
                .name("Raj Ghat")
                .city("New Delhi")
                .country("India")
                .status(RememberMe.ApprovalStatus.APPROVED)
                .build();

        when(rememberMeRepository.searchByCityAndCountryUser(eq("New Delhi"), eq("India"), any()))
                .thenReturn(List.of(memorial));
        when(deceasedPersonRepository.searchByCityAndCountryUser(eq("New Delhi"), eq("India"), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/search")
                        .param("city", "New Delhi")
                        .param("country", "India"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Records found"))
                .andExpect(jsonPath("$.data.rememberMes[0].city").value("New Delhi"));
    }

    @Test
    @DisplayName("Explicit person search type does not display 'No city or country found.' when no records match")
    public void search_ExplicitPersonType_NoMatch_PreservesPersonSearchBehavior() throws Exception {
        when(deceasedPersonRepository.findByFullNameContainingIgnoreCase(eq("UnknownPerson")))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/search")
                        .param("query", "UnknownPerson")
                        .param("type", "person"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.data.people").isEmpty())
                .andExpect(jsonPath("$.data.rememberMes").isEmpty());
    }
}
