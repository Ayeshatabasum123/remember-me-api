package com.rememberme.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rememberme.api.dto.request.RejectOrganizationRequest;
import com.rememberme.api.dto.response.OrganizationResponseDto;
import com.rememberme.api.entity.OrganizationApprovalStatus;
import com.rememberme.api.entity.OrganizationType;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtUtil;
import com.rememberme.api.service.OrganizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SuperAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SuperAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizationService organizationService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void getPendingOrganizations_Success() throws Exception {
        OrganizationResponseDto dto = OrganizationResponseDto.builder()
                .id(1L)
                .name("St. Paul's")
                .organizationType(OrganizationType.CHURCH)
                .approvalStatus(OrganizationApprovalStatus.PENDING)
                .build();

        when(organizationService.getPendingOrganizations()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/super-admin/organizations/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("St. Paul's"))
                .andExpect(jsonPath("$.data[0].approvalStatus").value("PENDING"));
    }

    @Test
    public void approveOrganization_Success() throws Exception {
        OrganizationResponseDto dto = OrganizationResponseDto.builder()
                .id(1L)
                .name("St. Paul's")
                .organizationType(OrganizationType.CHURCH)
                .organizationCode("ORG-000001")
                .approvalStatus(OrganizationApprovalStatus.APPROVED)
                .build();

        when(organizationService.approveOrganization(1L)).thenReturn(dto);

        mockMvc.perform(put("/api/super-admin/organizations/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.organizationCode").value("ORG-000001"))
                .andExpect(jsonPath("$.data.approvalStatus").value("APPROVED"));
    }

    @Test
    public void rejectOrganization_Success() throws Exception {
        OrganizationResponseDto dto = OrganizationResponseDto.builder()
                .id(1L)
                .name("St. Paul's")
                .approvalStatus(OrganizationApprovalStatus.REJECTED)
                .rejectionReason("Incomplete paperwork")
                .build();

        when(organizationService.rejectOrganization(1L, "Incomplete paperwork")).thenReturn(dto);

        RejectOrganizationRequest req = new RejectOrganizationRequest("Incomplete paperwork");

        mockMvc.perform(put("/api/super-admin/organizations/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.approvalStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Incomplete paperwork"));
    }
}
