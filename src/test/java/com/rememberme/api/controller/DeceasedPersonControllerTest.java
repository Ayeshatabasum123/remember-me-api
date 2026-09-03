package com.rememberme.api.controller;

import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.DeceasedPersonRepository;
import com.rememberme.api.repository.GraveRepository;
import com.rememberme.api.repository.MemorialRepository;
import com.rememberme.api.repository.RelationshipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtUtil;

@WebMvcTest(DeceasedPersonController.class)
@AutoConfigureMockMvc(addFilters = false)
public class DeceasedPersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeceasedPersonRepository deceasedPersonRepository;

    @MockBean
    private GraveRepository graveRepository;

    @MockBean
    private MemorialRepository memorialRepository;

    @MockBean
    private RelationshipRepository relationshipRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void deleteDeceasedPerson_Success() throws Exception {
        DeceasedPerson person = new DeceasedPerson();
        person.setId(1L);

        when(deceasedPersonRepository.findById(1L)).thenReturn(Optional.of(person));
        when(memorialRepository.existsByDeceasedPersonId(1L)).thenReturn(false);
        when(relationshipRepository.existsByDeceasedPersonId(1L)).thenReturn(false);
        doNothing().when(deceasedPersonRepository).delete(person);

        mockMvc.perform(delete("/api/deceased/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Deceased person record deleted successfully"));
    }

    @Test
    public void deleteDeceasedPerson_NotFound() throws Exception {
        when(deceasedPersonRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/deceased/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Deceased person not found with ID: 1"));
    }

    @Test
    public void deleteDeceasedPerson_Conflict_HasMemorial() throws Exception {
        DeceasedPerson person = new DeceasedPerson();
        person.setId(1L);

        when(deceasedPersonRepository.findById(1L)).thenReturn(Optional.of(person));
        when(memorialRepository.existsByDeceasedPersonId(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/deceased/{id}", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete deceased person: A memorial is linked to this person"));
    }

    @Test
    public void deleteDeceasedPerson_Conflict_HasRelationship() throws Exception {
        DeceasedPerson person = new DeceasedPerson();
        person.setId(1L);

        when(deceasedPersonRepository.findById(1L)).thenReturn(Optional.of(person));
        when(memorialRepository.existsByDeceasedPersonId(1L)).thenReturn(false);
        when(relationshipRepository.existsByDeceasedPersonId(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/deceased/{id}", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete deceased person: Active relationships are linked to this person"));
    }
}
