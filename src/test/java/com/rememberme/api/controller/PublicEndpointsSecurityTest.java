package com.rememberme.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rememberme.api.config.SecurityConfig;
import com.rememberme.api.dto.response.MemorialSummaryDto;
import com.rememberme.api.dto.response.RecentMemorialsResponseDto;
import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.Grave;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.entity.User;
import com.rememberme.api.repository.DeceasedPersonRepository;
import com.rememberme.api.repository.GraveRepository;
import com.rememberme.api.repository.MemorialRepository;
import com.rememberme.api.repository.RelationshipRepository;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtAuthFilter;
import com.rememberme.api.security.JwtUtil;
import com.rememberme.api.service.MemorialService;
import com.rememberme.api.service.NotificationManagementService;
import com.rememberme.api.service.PhotoUrlValidator;
import com.rememberme.api.service.PostcodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {RootController.class, DeceasedPersonController.class, MemorialController.class, UserController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class})
public class PublicEndpointsSecurityTest {

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
    private MemorialService memorialService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PostcodeService postcodeService;

    @MockBean
    private NotificationManagementService notificationManagementService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void root_WithoutJwt_Returns200WithApiStatusMessage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Remember Me API is running"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("Remember Me API"));
    }

    @Test
    public void apiRoot_WithoutJwt_Returns200() throws Exception {
        mockMvc.perform(get("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Remember Me API is running"));
    }

    @Test
    public void getDeceased_WithoutJwt_Returns200() throws Exception {
        when(deceasedPersonRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/deceased"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    public void getRecentMemorialsV1_WithoutJwt_Returns200() throws Exception {
        RecentMemorialsResponseDto mockResponse = RecentMemorialsResponseDto.builder()
                .memorials(List.of(
                        MemorialSummaryDto.builder()
                                .memorialId(1L)
                                .deceasedName("Jane Doe")
                                .dateOfBirth(LocalDate.of(1960, 1, 1))
                                .dateOfDeath(LocalDate.of(2020, 1, 1))
                                .biography("Loving mother and friend")
                                .build()
                ))
                .pagination(RecentMemorialsResponseDto.PaginationDto.builder()
                        .currentPage(0)
                        .totalPages(1)
                        .totalRecords(1L)
                        .pageSize(10)
                        .hasNext(false)
                        .build())
                .build();

        when(memorialService.getRecentMemorialSummaries(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/memorials/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memorials[0].deceasedName").value("Jane Doe"));
    }

    @Test
    public void getRecentMemorialsLegacy_WithoutJwt_Returns200() throws Exception {
        RecentMemorialsResponseDto mockResponse = RecentMemorialsResponseDto.builder()
                .memorials(Collections.emptyList())
                .pagination(RecentMemorialsResponseDto.PaginationDto.builder().build())
                .build();

        when(memorialService.getRecentMemorialSummaries(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/memorials/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void postDeceased_WithoutJwt_Returns401Unauthorized() throws Exception {
        mockMvc.perform(post("/api/deceased")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Test Person\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    public void putDeceased_WithoutJwt_Returns401Unauthorized() throws Exception {
        mockMvc.perform(put("/api/deceased/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Updated Person\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    public void deleteDeceased_WithoutJwt_Returns401Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/deceased/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    public void postMemorial_WithoutJwt_Returns401Unauthorized() throws Exception {
        mockMvc.perform(post("/api/memorials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"biography\":\"Test Biography\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    public void getUserProfile_WithoutJwt_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    public void root_WithInvalidBearerToken_DoesNotRejectAndReturns200() throws Exception {
        when(jwtUtil.isTokenValid("invalid-expired-token")).thenReturn(false);

        mockMvc.perform(get("/")
                        .header("Authorization", "Bearer invalid-expired-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Remember Me API is running"));
    }

    @Test
    public void getDeceased_WithInvalidBearerToken_DoesNotRejectAndReturns200() throws Exception {
        when(jwtUtil.isTokenValid("invalid-expired-token")).thenReturn(false);
        when(deceasedPersonRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/deceased")
                        .header("Authorization", "Bearer invalid-expired-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void getUserProfile_WithInvalidBearerToken_Returns401Unauthorized() throws Exception {
        when(jwtUtil.isTokenValid("invalid-expired-token")).thenReturn(false);

        mockMvc.perform(get("/api/users/1")
                        .header("Authorization", "Bearer invalid-expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    public void deceasedSerialization_DoesNotExposePrivateUserData() throws Exception {
        User sensitiveUser = User.builder()
                .id(99L)
                .fullName("Admin Creator")
                .email("private.creator@example.com")
                .phone("+447123456789")
                .fcmToken("secret-push-fcm-token")
                .password("hashed_password_123")
                .postcode("SW1A 1AA")
                .build();

        RememberMe graveyard = RememberMe.builder()
                .id(1L)
                .name("City Cemetery")
                .city("London")
                .country("United Kingdom")
                .latitude(51.5074)
                .longitude(-0.1278)
                .managedBy(sensitiveUser)
                .build();

        Grave grave = Grave.builder()
                .id(10L)
                .rememberMe(graveyard)
                .graveNumber("G-101")
                .latitude(51.5074)
                .longitude(-0.1278)
                .build();

        DeceasedPerson person = DeceasedPerson.builder()
                .id(5L)
                .fullName("Arthur Vance")
                .grave(grave)
                .addedBy(sensitiveUser)
                .build();

        String json = objectMapper.writeValueAsString(person);

        assertFalse(json.contains("private.creator@example.com"), "Private email must not be exposed in deceased serialization");
        assertFalse(json.contains("+447123456789"), "Private phone must not be exposed in deceased serialization");
        assertFalse(json.contains("secret-push-fcm-token"), "FCM token must not be exposed in deceased serialization");
        assertFalse(json.contains("hashed_password_123"), "Password must not be exposed in deceased serialization");
    }
}
