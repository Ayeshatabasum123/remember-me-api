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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rememberme.api.repository.DeceasedPersonRepository;

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
    private DeceasedPersonRepository deceasedPersonRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void deleteMemorial_Success() throws Exception {
        org.mockito.Mockito.doNothing().when(memorialService).deleteMemorial(1L);

        mockMvc.perform(delete("/api/memorials/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Memorial deleted successfully"));
    }

    @Test
    public void deleteMemorial_NotFound() throws Exception {
        org.mockito.Mockito.doThrow(new com.rememberme.api.exception.ApiException("Memorial not found with ID: 999", org.springframework.http.HttpStatus.NOT_FOUND))
                .when(memorialService).deleteMemorial(999L);

        mockMvc.perform(delete("/api/memorials/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Memorial not found with ID: 999"));
    }

    @Test
    public void getRecentMemorialSummaries_Success() throws Exception {
        com.rememberme.api.dto.response.MemorialSummaryDto dto = com.rememberme.api.dto.response.MemorialSummaryDto.builder()
                .memorialId(1L)
                .deceasedName("John Doe")
                .photoUrl("https://s3.amazonaws.com/bucket/john.jpg")
                .dateOfBirth(java.time.LocalDate.of(1950, 1, 1))
                .dateOfDeath(java.time.LocalDate.of(2020, 5, 15))
                .biography("Test tribute message")
                .createdAt(java.time.LocalDateTime.of(2026, 8, 30, 10, 0))
                .build();

        com.rememberme.api.dto.response.RecentMemorialsResponseDto response =
                com.rememberme.api.dto.response.RecentMemorialsResponseDto.builder()
                        .memorials(java.util.List.of(dto))
                        .pagination(com.rememberme.api.dto.response.RecentMemorialsResponseDto.PaginationDto.builder()
                                .currentPage(0)
                                .pageSize(10)
                                .totalRecords(143)
                                .totalPages(15)
                                .hasNext(true)
                                .build())
                        .build();

        when(memorialService.getRecentMemorialSummaries(org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").descending())))
                .thenReturn(response);

        mockMvc.perform(get("/api/memorials/recent")
                        .param("page", "0")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memorials[0].memorialId").value(1))
                .andExpect(jsonPath("$.data.memorials[0].deceasedName").value("John Doe"))
                .andExpect(jsonPath("$.data.memorials[0].photoUrl").value("https://s3.amazonaws.com/bucket/john.jpg"))
                .andExpect(jsonPath("$.data.memorials[0].dateOfBirth").value("1950-01-01"))
                .andExpect(jsonPath("$.data.memorials[0].dateOfDeath").value("2020-05-15"))
                .andExpect(jsonPath("$.data.memorials[0].biography").value("Test tribute message"))
                .andExpect(jsonPath("$.data.pagination.currentPage").value(0))
                .andExpect(jsonPath("$.data.pagination.pageSize").value(10))
                .andExpect(jsonPath("$.data.pagination.totalRecords").value(143))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(15))
                .andExpect(jsonPath("$.data.pagination.hasNext").value(true));
    }

    @Test
    public void getRecentMemorialSummaries_InvalidPage_Returns400() throws Exception {
        mockMvc.perform(get("/api/memorials/recent")
                        .param("page", "-1")
                        .param("limit", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PAGINATION"))
                .andExpect(jsonPath("$.error.message").value("Page index must be non-negative and limit must be between 1 and 50"));
    }

    @Test
    public void getRecentMemorialSummaries_InvalidLimitExceedsMax_Returns400() throws Exception {
        mockMvc.perform(get("/api/memorials/recent")
                        .param("page", "0")
                        .param("limit", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PAGINATION"))
                .andExpect(jsonPath("$.error.message").value("Page index must be non-negative and limit must be between 1 and 50"));
    }

    @Test
    public void getRecentMemorialSummaries_InvalidLimitZero_Returns400() throws Exception {
        mockMvc.perform(get("/api/memorials/recent")
                        .param("page", "0")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PAGINATION"));
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

    @Test
    public void deserializeMemorial_Success() throws Exception {
        String json = "{\n" +
                "  \"id\": 14,\n" +
                "  \"deceasedPerson\": {\n" +
                "    \"id\": 14,\n" +
                "    \"fullName\": \"Mahatma Gandhi\",\n" +
                "    \"dateOfBirth\": \"1869-10-02\",\n" +
                "    \"dateOfDeath\": \"1948-01-30\",\n" +
                "    \"gender\": \"MALE\",\n" +
                "    \"photoUrl\": \"https://live.staticflickr.com/84/255569844_3760184197_o.jpg\",\n" +
                "    \"grave\": {\n" +
                "      \"id\": 14,\n" +
                "      \"rememberMe\": {\n" +
                "        \"id\": 18\n" +
                "      },\n" +
                "      \"graveNumber\": \"N/A\",\n" +
                "      \"section\": \"N/A\",\n" +
                "      \"row\": \"N/A\",\n" +
                "      \"latitude\": 28.6406607,\n" +
                "      \"longitude\": 77.249518,\n" +
                "      \"locationAccuracy\": null,\n" +
                "      \"verificationStatus\": \"UNVERIFIED\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"biography\": \"Mahatma Gandhi...\",\n" +
                "  \"prayerOrMessage\": \"May the legacy...\"\n" +
                "}";

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        Memorial memorial = mapper.readValue(json, Memorial.class);
        org.junit.jupiter.api.Assertions.assertNotNull(memorial);
        org.junit.jupiter.api.Assertions.assertNotNull(memorial.getDeceasedPerson());
        org.junit.jupiter.api.Assertions.assertNotNull(memorial.getDeceasedPerson().getGrave());
        org.junit.jupiter.api.Assertions.assertNotNull(memorial.getDeceasedPerson().getGrave().getRememberMe());
    }

    @Test
    public void postMemorial_MockMvc_Success() throws Exception {
        String json = "{\n" +
                "  \"id\": 14,\n" +
                "  \"deceasedPerson\": {\n" +
                "    \"id\": 14,\n" +
                "    \"fullName\": \"Mahatma Gandhi\",\n" +
                "    \"dateOfBirth\": \"1869-10-02\",\n" +
                "    \"dateOfDeath\": \"1948-01-30\",\n" +
                "    \"gender\": \"MALE\",\n" +
                "    \"photoUrl\": \"https://live.staticflickr.com/84/255569844_3760184197_o.jpg\",\n" +
                "    \"grave\": {\n" +
                "      \"id\": 14,\n" +
                "      \"rememberMe\": {\n" +
                "        \"id\": 18\n" +
                "      },\n" +
                "      \"graveNumber\": \"N/A\",\n" +
                "      \"section\": \"N/A\",\n" +
                "      \"row\": \"N/A\",\n" +
                "      \"latitude\": 28.6406607,\n" +
                "      \"longitude\": 77.249518,\n" +
                "      \"locationAccuracy\": null,\n" +
                "      \"verificationStatus\": \"UNVERIFIED\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"biography\": \"Mahatma Gandhi...\",\n" +
                "  \"prayerOrMessage\": \"May the legacy...\"\n" +
                "}";

        when(memorialRepository.save(org.mockito.ArgumentMatchers.any(Memorial.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/memorials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void putMemorial_MockMvc_Success() throws Exception {
        String json = "{\n" +
                "  \"id\": 14,\n" +
                "  \"deceasedPerson\": {\n" +
                "    \"id\": 14,\n" +
                "    \"fullName\": \"Mahatma Gandhi\",\n" +
                "    \"dateOfBirth\": \"1869-10-02\",\n" +
                "    \"dateOfDeath\": \"1948-01-30\",\n" +
                "    \"gender\": \"MALE\",\n" +
                "    \"photoUrl\": \"https://live.staticflickr.com/84/255569844_3760184197_o.jpg\",\n" +
                "    \"grave\": {\n" +
                "      \"id\": 14,\n" +
                "      \"rememberMe\": {\n" +
                "        \"id\": 18\n" +
                "      },\n" +
                "      \"graveNumber\": \"N/A\",\n" +
                "      \"section\": \"N/A\",\n" +
                "      \"row\": \"N/A\",\n" +
                "      \"latitude\": 28.6406607,\n" +
                "      \"longitude\": 77.249518,\n" +
                "      \"locationAccuracy\": null,\n" +
                "      \"verificationStatus\": \"UNVERIFIED\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"biography\": \"Mahatma Gandhi...\",\n" +
                "  \"prayerOrMessage\": \"May the legacy...\"\n" +
                "}";

        Memorial memorial = Memorial.builder().id(14L).biography("Old").prayerOrMessage("Old").build();
        when(memorialRepository.findById(14L)).thenReturn(Optional.of(memorial));
        when(memorialRepository.save(org.mockito.ArgumentMatchers.any(Memorial.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/memorials/{id}", 14L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
