package com.rememberme.api.controller;

import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.Memorial;
import com.rememberme.api.repository.MemorialRepository;
import com.rememberme.api.service.MemorialService;
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
    private MemorialService memorialService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void getRecentMemorials_Success() throws Exception {
        com.rememberme.api.dto.response.RecentMemorialDto dto = com.rememberme.api.dto.response.RecentMemorialDto.builder()
                .memorialId(1L)
                .name("John Doe")
                .biography("Test biography")
                .hasMoreBio(false)
                .build();

        com.rememberme.api.dto.response.PaginatedResponse<com.rememberme.api.dto.response.RecentMemorialDto> response =
                com.rememberme.api.dto.response.PaginatedResponse.<com.rememberme.api.dto.response.RecentMemorialDto>builder()
                        .content(java.util.List.of(dto))
                        .pagination(com.rememberme.api.dto.response.PaginatedResponse.PaginationMetadata.builder()
                                .page(0)
                                .size(10)
                                .totalElements(1)
                                .totalPages(1)
                                .hasNext(false)
                                .build())
                        .build();

        when(memorialService.getRecentMemorials(org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").descending())))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/memorials/recent")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].memorialId").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("John Doe"))
                .andExpect(jsonPath("$.data.content[0].biography").value("Test biography"))
                .andExpect(jsonPath("$.data.pagination.page").value(0))
                .andExpect(jsonPath("$.data.pagination.size").value(10))
                .andExpect(jsonPath("$.data.pagination.totalElements").value(1));
    }

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
