package com.rememberme.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rememberme.api.dto.request.AdminRegisterRequest;
import com.rememberme.api.dto.request.CreateEventRequest;
import com.rememberme.api.dto.request.UpdateEventRequest;
import com.rememberme.api.dto.response.AdminRegisterResponse;
import com.rememberme.api.dto.response.EventResponseDto;
import com.rememberme.api.dto.response.OrganizationResponseDto;
import com.rememberme.api.entity.EventStatus;
import com.rememberme.api.entity.EventType;
import com.rememberme.api.entity.OrganizationApprovalStatus;
import com.rememberme.api.entity.OrganizationType;
import com.rememberme.api.repository.RememberMeRepository;
import com.rememberme.api.repository.ReportRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtUtil;
import com.rememberme.api.service.EventService;
import com.rememberme.api.service.GraveImportService;
import com.rememberme.api.service.OrganizationService;
import com.rememberme.api.service.PhotoAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminRegistrationAndEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizationService organizationService;

    @MockBean
    private EventService eventService;

    @MockBean
    private RememberMeRepository rememberMeRepository;

    @MockBean
    private ReportRepository reportRepository;

    @MockBean
    private GraveImportService graveImportService;

    @MockBean
    private PhotoAuditService photoAuditService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void registerAdminJson_Success() throws Exception {
        AdminRegisterRequest request = AdminRegisterRequest.builder()
                .organizationName("London Central Mosque")
                .organizationType(OrganizationType.MOSQUE)
                .email("admin@lcm.org")
                .phone("+442079461111")
                .password("Password123!")
                .confirmPassword("Password123!")
                .postcode("NW8 7RG")
                .latitude(51.53)
                .longitude(-0.16)
                .photoUrl("https://example.com/photo.jpg")
                .build();

        AdminRegisterResponse response = AdminRegisterResponse.builder()
                .userId(1L)
                .email("admin@lcm.org")
                .role("ADMIN")
                .organization(OrganizationResponseDto.builder()
                        .name("London Central Mosque")
                        .approvalStatus(OrganizationApprovalStatus.PENDING)
                        .build())
                .build();

        when(organizationService.registerAdminAndOrganization(any(AdminRegisterRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("admin@lcm.org"))
                .andExpect(jsonPath("$.data.organization.approvalStatus").value("PENDING"));
    }

    @Test
    public void registerAdminMultipart_Success() throws Exception {
        MockMultipartFile photo = new MockMultipartFile("photograph", "church.jpg", "image/jpeg", "image_bytes".getBytes());

        AdminRegisterResponse response = AdminRegisterResponse.builder()
                .userId(2L)
                .email("admin@church.org")
                .role("ADMIN")
                .organization(OrganizationResponseDto.builder()
                        .name("St Church")
                        .approvalStatus(OrganizationApprovalStatus.PENDING)
                        .build())
                .build();

        when(organizationService.registerAdminAndOrganization(any(AdminRegisterRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/admin/register")
                        .file(photo)
                        .param("organizationName", "St Church")
                        .param("organizationType", "CHURCH")
                        .param("email", "admin@church.org")
                        .param("phone", "+442079461111")
                        .param("password", "Password123!")
                        .param("confirmPassword", "Password123!")
                        .param("postcode", "SW1A 1AA")
                        .param("latitude", "51.501")
                        .param("longitude", "-0.141"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("admin@church.org"));
    }

    @Test
    @WithMockUser(username = "admin@lcm.org", roles = {"ADMIN"})
    public void createEvent_Success() throws Exception {
        CreateEventRequest request = CreateEventRequest.builder()
                .title("Funeral Announcement")
                .description("Service details")
                .eventType(EventType.FUNERAL)
                .eventDateTime(OffsetDateTime.now())
                .build();

        EventResponseDto responseDto = EventResponseDto.builder()
                .id(10L)
                .title("Funeral Announcement")
                .eventType(EventType.FUNERAL)
                .status(EventStatus.DRAFT)
                .build();

        when(eventService.createEvent(eq("admin@lcm.org"), any(CreateEventRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/admin/events")
                        .principal(() -> "admin@lcm.org")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(username = "admin@lcm.org", roles = {"ADMIN"})
    public void updateEvent_Success() throws Exception {
        UpdateEventRequest request = UpdateEventRequest.builder()
                .title("Updated Title")
                .description("Updated details")
                .eventType(EventType.FUNERAL)
                .eventDateTime(OffsetDateTime.now())
                .build();

        EventResponseDto responseDto = EventResponseDto.builder()
                .id(10L)
                .title("Updated Title")
                .eventType(EventType.FUNERAL)
                .status(EventStatus.DRAFT)
                .build();

        when(eventService.updateEvent(eq("admin@lcm.org"), eq(10L), any(UpdateEventRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/api/admin/events/10")
                        .principal(() -> "admin@lcm.org")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Title"));
    }

    @Test
    @WithMockUser(username = "admin@lcm.org", roles = {"ADMIN"})
    public void publishEvent_Success() throws Exception {
        EventResponseDto responseDto = EventResponseDto.builder()
                .id(10L)
                .title("Published Funeral Announcement")
                .eventType(EventType.FUNERAL)
                .status(EventStatus.PUBLISHED)
                .build();

        when(eventService.publishEvent(eq("admin@lcm.org"), eq(10L)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/admin/events/10/publish")
                        .principal(() -> "admin@lcm.org"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }
}
