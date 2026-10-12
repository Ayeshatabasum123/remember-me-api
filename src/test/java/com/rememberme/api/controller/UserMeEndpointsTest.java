package com.rememberme.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rememberme.api.dto.request.RegisterDeviceTokenRequest;
import com.rememberme.api.dto.request.UserPostcodeRequest;
import com.rememberme.api.dto.response.NotificationResponseDto;
import com.rememberme.api.dto.response.PostcodeValidationResponse;
import com.rememberme.api.entity.EventType;
import com.rememberme.api.entity.User;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtUtil;
import com.rememberme.api.service.NotificationManagementService;
import com.rememberme.api.service.PostcodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserMeEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    @WithMockUser(username = "user@test.com")
    public void updateMyPostcode_Success() throws Exception {
        User user = User.builder().id(1L).email("user@test.com").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        PostcodeValidationResponse val = PostcodeValidationResponse.builder()
                .valid(true)
                .postcode("SW1A 1AA")
                .build();
        when(postcodeService.validatePostcode("SW1A 1AA")).thenReturn(val);
        when(postcodeService.normalizePostcode("SW1A 1AA")).thenReturn("SW1A1AA");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserPostcodeRequest req = new UserPostcodeRequest("SW1A 1AA");

        mockMvc.perform(put("/api/users/me/postcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.postcode").value("SW1A 1AA"))
                .andExpect(jsonPath("$.data.postcodeNormalized").value("SW1A1AA"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    public void registerDeviceToken_Success() throws Exception {
        User user = User.builder().id(1L).email("user@test.com").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        RegisterDeviceTokenRequest req = new RegisterDeviceTokenRequest("device_token_xyz");

        mockMvc.perform(post("/api/users/me/device-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationManagementService, times(1)).registerDeviceToken(user, "device_token_xyz");
    }

    @Test
    @WithMockUser(username = "user@test.com")
    public void getMyNotifications_Success() throws Exception {
        User user = User.builder().id(1L).email("user@test.com").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        NotificationResponseDto notification = NotificationResponseDto.builder()
                .notificationId(10L)
                .eventId(2L)
                .organizationName("Local Mosque")
                .eventTitle("Funeral Announcement")
                .eventDescription("Funeral prayer details")
                .eventType(EventType.FUNERAL)
                .createdDate(LocalDateTime.now())
                .readStatus(false)
                .deliveryStatus("SENT")
                .build();

        when(notificationManagementService.getUserNotifications(eq(user), any()))
                .thenReturn(new PageImpl<>(List.of(notification)));

        mockMvc.perform(get("/api/users/me/notifications?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].notificationId").value(10L))
                .andExpect(jsonPath("$.data.content[0].organizationName").value("Local Mosque"))
                .andExpect(jsonPath("$.data.content[0].eventTitle").value("Funeral Announcement"));
    }
}
