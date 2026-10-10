package com.rememberme.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rememberme.api.dto.request.DeceasedPersonRequest;
import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.Grave;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.DeceasedPersonRepository;
import com.rememberme.api.repository.GraveRepository;
import com.rememberme.api.repository.MemorialRepository;
import com.rememberme.api.repository.RelationshipRepository;
import com.rememberme.api.service.PhotoUrlValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtUtil;

@WebMvcTest(DeceasedPersonController.class)
@AutoConfigureMockMvc(addFilters = false)
public class DeceasedPersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeceasedPersonRepository deceasedPersonRepository;

    @MockBean
    private GraveRepository graveRepository;

    @MockBean
    private MemorialRepository memorialRepository;

    @MockBean
    private RelationshipRepository relationshipRepository;

    @MockBean
    private PhotoUrlValidator photoUrlValidator;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void addDeceasedPerson_ValidPhotoUrl_PersistsResolvedUrl() throws Exception {
        Grave grave = Grave.builder().id(10L).build();
        when(graveRepository.findById(10L)).thenReturn(Optional.of(grave));

        String rawUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/Sample.jpg";
        String resolvedUrl = "https://upload.wikimedia.org/wikipedia/commons/Sample.jpg";
        when(photoUrlValidator.validatePhotoUrl(rawUrl))
                .thenReturn(PhotoUrlValidator.ValidationResult.success(resolvedUrl));

        when(deceasedPersonRepository.save(any(DeceasedPerson.class))).thenAnswer(i -> {
            DeceasedPerson dp = i.getArgument(0);
            dp.setId(100L);
            return dp;
        });

        DeceasedPersonRequest request = new DeceasedPersonRequest();
        request.setGraveId(10L);
        request.setFullName("John Doe");
        request.setDateOfBirth(LocalDate.of(1950, 1, 1));
        request.setDateOfDeath(LocalDate.of(2020, 1, 1));
        request.setPhotoUrl(rawUrl);

        mockMvc.perform(post("/api/deceased")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.photoUrl").value(resolvedUrl));
    }

    @Test
    public void addDeceasedPerson_InvalidPhotoUrl_ReturnsBadRequest() throws Exception {
        Grave grave = Grave.builder().id(10L).build();
        when(graveRepository.findById(10L)).thenReturn(Optional.of(grave));

        String invalidUrl = "https://example.com/oversized.jpg";
        when(photoUrlValidator.validatePhotoUrl(invalidUrl))
                .thenReturn(PhotoUrlValidator.ValidationResult.failure("Image size must not exceed 1 MB."));

        DeceasedPersonRequest request = new DeceasedPersonRequest();
        request.setGraveId(10L);
        request.setFullName("John Doe");
        request.setPhotoUrl(invalidUrl);

        mockMvc.perform(post("/api/deceased")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Image size must not exceed 1 MB."));
    }

    @Test
    public void updateDeceasedPerson_ValidPhotoUrl_Success() throws Exception {
        Grave grave = Grave.builder().id(10L).build();
        DeceasedPerson existing = DeceasedPerson.builder()
                .id(50L)
                .fullName("Old Name")
                .grave(grave)
                .build();
        when(deceasedPersonRepository.findById(50L)).thenReturn(Optional.of(existing));

        String newUrl = "https://example.com/valid.jpg";
        when(photoUrlValidator.validatePhotoUrl(newUrl))
                .thenReturn(PhotoUrlValidator.ValidationResult.success(newUrl));
        when(deceasedPersonRepository.save(any(DeceasedPerson.class))).thenAnswer(i -> i.getArgument(0));

        DeceasedPersonRequest request = new DeceasedPersonRequest();
        request.setFullName("Updated Name");
        request.setPhotoUrl(newUrl);

        mockMvc.perform(put("/api/deceased/{id}", 50L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.data.photoUrl").value(newUrl));
    }

    @Test
    public void updateDeceasedPerson_InvalidPhotoUrl_ReturnsBadRequest() throws Exception {
        DeceasedPerson existing = DeceasedPerson.builder()
                .id(50L)
                .fullName("Existing")
                .build();
        when(deceasedPersonRepository.findById(50L)).thenReturn(Optional.of(existing));

        String invalidUrl = "https://example.com/photo.png";
        when(photoUrlValidator.validatePhotoUrl(invalidUrl))
                .thenReturn(PhotoUrlValidator.ValidationResult.failure("Only JPG and JPEG image URLs are allowed."));

        DeceasedPersonRequest request = new DeceasedPersonRequest();
        request.setPhotoUrl(invalidUrl);

        mockMvc.perform(put("/api/deceased/{id}", 50L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only JPG and JPEG image URLs are allowed."));
    }

    @Test
    public void deleteDeceasedPerson_Success() throws Exception {
        DeceasedPerson person = new DeceasedPerson();
        person.setId(1L);

        when(deceasedPersonRepository.findById(1L)).thenReturn(Optional.of(person));
        doNothing().when(memorialRepository).deleteByDeceasedPersonId(1L);
        doNothing().when(relationshipRepository).deleteByDeceasedPersonId(1L);
        doNothing().when(deceasedPersonRepository).delete(person);

        mockMvc.perform(delete("/api/deceased/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Deceased person record deleted successfully"));

        verify(memorialRepository).deleteByDeceasedPersonId(1L);
        verify(relationshipRepository).deleteByDeceasedPersonId(1L);
        verify(deceasedPersonRepository).delete(person);
    }

    @Test
    public void deleteDeceasedPerson_NotFound() throws Exception {
        when(deceasedPersonRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/deceased/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Deceased person not found with ID: 1"));
    }

    @Test
    public void deleteDeceasedPerson_Success_WithAssociatedMemorialAndRelationships() throws Exception {
        DeceasedPerson person = new DeceasedPerson();
        person.setId(2L);

        when(deceasedPersonRepository.findById(2L)).thenReturn(Optional.of(person));
        doNothing().when(memorialRepository).deleteByDeceasedPersonId(2L);
        doNothing().when(relationshipRepository).deleteByDeceasedPersonId(2L);
        doNothing().when(deceasedPersonRepository).delete(person);

        mockMvc.perform(delete("/api/deceased/{id}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Deceased person record deleted successfully"));

        verify(memorialRepository).deleteByDeceasedPersonId(2L);
        verify(relationshipRepository).deleteByDeceasedPersonId(2L);
        verify(deceasedPersonRepository).delete(person);
    }
}
