package com.rememberme.api.communityfund.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rememberme.api.communityfund.dto.request.CreateSupportRequestDto;
import com.rememberme.api.communityfund.dto.request.UpdateSupportRequestDto;
import com.rememberme.api.communityfund.dto.response.*;
import com.rememberme.api.communityfund.enums.SupportDocumentType;
import com.rememberme.api.communityfund.enums.SupportRequestStatus;
import com.rememberme.api.communityfund.enums.SupportUrgency;
import com.rememberme.api.communityfund.security.CommunityFundSecurityHelper;
import com.rememberme.api.communityfund.service.SupportRequestService;
import com.rememberme.api.communityfund.service.SupportTrackingService;
import com.rememberme.api.entity.User;
import com.rememberme.api.repository.UserRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SupportRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SupportRequestControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SupportRequestService supportRequestService;

    @MockBean
    private SupportTrackingService supportTrackingService;

    @MockBean
    private CommunityFundSecurityHelper securityHelper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("ahmed@example.com")
                .fullName("Ahmed Khan")
                .role(User.Role.USER)
                .build();

        when(securityHelper.getCurrentAuthenticatedUser()).thenReturn(testUser);
    }

    @Test
    @DisplayName("POST /api/support-requests creates request and returns expected JSON")
    void testCreateSupportRequest_Success() throws Exception {
        CreateSupportRequestDto dto = CreateSupportRequestDto.builder()
                .applicantName("Ahmed Khan")
                .email("ahmed@example.com")
                .phone("+447700900123")
                .relationshipToDeceased("Son")
                .authorityToApply(true)
                .burialProviderName("Example Funeral Services")
                .burialProviderContact("+447700900456")
                .totalCost(new BigDecimal("3000.00"))
                .otherFunding(new BigDecimal("1000.00"))
                .requestedAmount(new BigDecimal("2000.00"))
                .currency("GBP")
                .urgency(SupportUrgency.HIGH)
                .supportingNotes("Financial assistance required for burial expenses.")
                .build();

        SupportRequestCreateResponseDto responseDto = SupportRequestCreateResponseDto.builder()
                .id(1L)
                .status(SupportRequestStatus.PENDING)
                .requestedAmount(new BigDecimal("2000.00"))
                .currency("GBP")
                .build();

        when(supportRequestService.createSupportRequest(eq(testUser), any(CreateSupportRequestDto.class), any()))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/support-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Support request submitted successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.requestedAmount").value(2000.00))
                .andExpect(jsonPath("$.data.currency").value("GBP"));
    }

    @Test
    @DisplayName("POST /api/support-requests with missing fields returns 400 Bad Request")
    void testCreateSupportRequest_ValidationFailure() throws Exception {
        CreateSupportRequestDto dto = CreateSupportRequestDto.builder()
                .applicantName("") // blank name
                .email("invalid-email")
                .build();

        mockMvc.perform(post("/api/support-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/support-requests/{id} returns request details")
    void testGetSupportRequest_Success() throws Exception {
        SupportRequestResponseDto responseDto = SupportRequestResponseDto.builder()
                .id(1L)
                .applicantUserId(1L)
                .applicantName("Ahmed Khan")
                .email("ahmed@example.com")
                .phone("+447700900123")
                .relationshipToDeceased("Son")
                .authorityToApply(true)
                .burialProviderName("Example Funeral Services")
                .burialProviderContact("+447700900456")
                .totalCost(new BigDecimal("3000.00"))
                .otherFunding(new BigDecimal("1000.00"))
                .eligibleOutstandingCost(new BigDecimal("2000.00"))
                .requestedAmount(new BigDecimal("2000.00"))
                .currency("GBP")
                .urgency(SupportUrgency.HIGH)
                .status(SupportRequestStatus.PENDING)
                .documents(List.of())
                .createdAt(LocalDateTime.now())
                .build();

        when(supportRequestService.getSupportRequest(eq(1L), eq(testUser)))
                .thenReturn(responseDto);

        mockMvc.perform(get("/api/support-requests/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.applicantName").value("Ahmed Khan"))
                .andExpect(jsonPath("$.data.eligibleOutstandingCost").value(2000.00));
    }

    @Test
    @DisplayName("PUT /api/support-requests/{id} updates editable request")
    void testUpdateSupportRequest_Success() throws Exception {
        UpdateSupportRequestDto updateDto = UpdateSupportRequestDto.builder()
                .applicantName("Ahmed K. Khan")
                .totalCost(new BigDecimal("3200.00"))
                .requestedAmount(new BigDecimal("2200.00"))
                .build();

        SupportRequestResponseDto responseDto = SupportRequestResponseDto.builder()
                .id(1L)
                .applicantName("Ahmed K. Khan")
                .totalCost(new BigDecimal("3200.00"))
                .requestedAmount(new BigDecimal("2200.00"))
                .status(SupportRequestStatus.PENDING)
                .build();

        when(supportRequestService.updateSupportRequest(eq(1L), any(UpdateSupportRequestDto.class), eq(testUser)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/api/support-requests/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applicantName").value("Ahmed K. Khan"));
    }

    @Test
    @DisplayName("POST /api/support-requests/{id}/documents uploads multipart file")
    void testUploadDocument_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "application/pdf",
                "%PDF-1.4 sample content".getBytes()
        );

        SupportDocumentResponseDto docDto = SupportDocumentResponseDto.builder()
                .id(10L)
                .supportRequestId(1L)
                .documentType(SupportDocumentType.INVOICE)
                .originalFilename("invoice.pdf")
                .contentType("application/pdf")
                .fileSize(file.getSize())
                .uploadedByUserId(1L)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(supportRequestService.uploadDocument(eq(1L), any(), eq(SupportDocumentType.INVOICE), eq(testUser)))
                .thenReturn(docDto);

        mockMvc.perform(multipart("/api/support-requests/1/documents")
                        .file(file)
                        .param("documentType", "INVOICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.documentType").value("INVOICE"))
                .andExpect(jsonPath("$.data.originalFilename").value("invoice.pdf"));
    }

    @Test
    @DisplayName("GET /api/support-requests/{id}/documents lists uploaded documents")
    void testListDocuments_Success() throws Exception {
        SupportDocumentResponseDto docDto = SupportDocumentResponseDto.builder()
                .id(10L)
                .supportRequestId(1L)
                .documentType(SupportDocumentType.INVOICE)
                .originalFilename("invoice.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .uploadedByUserId(1L)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(supportRequestService.listDocuments(eq(1L), eq(testUser)))
                .thenReturn(List.of(docDto));

        mockMvc.perform(get("/api/support-requests/1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].originalFilename").value("invoice.pdf"));
    }

    @Test
    @DisplayName("GET /api/support-requests/{id}/documents/{docId} streams document")
    void testDownloadDocument_Success() throws Exception {
        byte[] content = "%PDF-1.4 file content".getBytes();
        SupportRequestService.DownloadedDocument downloaded =
                new SupportRequestService.DownloadedDocument(new ByteArrayResource(content), "invoice.pdf", "application/pdf", content.length);

        when(supportRequestService.downloadDocument(eq(1L), eq(10L), eq(testUser)))
                .thenReturn(downloaded);

        mockMvc.perform(get("/api/support-requests/1/documents/10"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"invoice.pdf\""))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(content));
    }

    @Test
    @DisplayName("DELETE /api/support-requests/{id}/documents/{docId} deletes document")
    void testDeleteDocument_Success() throws Exception {
        doNothing().when(supportRequestService).deleteDocument(eq(1L), eq(10L), eq(testUser));

        mockMvc.perform(delete("/api/support-requests/1/documents/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document deleted successfully"));
    }

    @Test
    @DisplayName("GET /api/support-requests/me returns logged-in user applications")
    void testGetMyApplications_Success() throws Exception {
        SupportRequestSummaryDto summary = SupportRequestSummaryDto.builder()
                .id(1L)
                .requestedAmount(new BigDecimal("2000.00"))
                .currency("GBP")
                .status(SupportRequestStatus.UNDER_REVIEW)
                .createdAt(LocalDateTime.now())
                .build();

        org.springframework.data.domain.Page<SupportRequestSummaryDto> page =
                new org.springframework.data.domain.PageImpl<>(List.of(summary));

        when(supportTrackingService.getMyApplications(eq(testUser), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(page);

        mockMvc.perform(get("/api/support-requests/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.data.content[0].requestedAmount").value(2000.00));
    }

    @Test
    @DisplayName("GET /api/support-requests/{id}/status returns application status and message")
    void testGetRequestStatus_Success() throws Exception {
        SupportRequestStatusResponseDto statusDto = SupportRequestStatusResponseDto.builder()
                .requestId(1L)
                .status(SupportRequestStatus.MORE_INFO_REQUIRED)
                .message("Additional invoice details are required.")
                .build();

        when(supportTrackingService.getRequestStatus(eq(1L), eq(testUser)))
                .thenReturn(statusDto);

        mockMvc.perform(get("/api/support-requests/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requestId").value(1))
                .andExpect(jsonPath("$.data.status").value("MORE_INFO_REQUIRED"))
                .andExpect(jsonPath("$.data.message").value("Additional invoice details are required."));
    }

    @Test
    @DisplayName("GET /api/support-requests/{id}/updates returns updates list")
    void testGetRequestUpdates_Success() throws Exception {
        SupportRequestUpdateResponseDto updateDto = SupportRequestUpdateResponseDto.builder()
                .id(5L)
                .supportRequestId(1L)
                .previousStatus(SupportRequestStatus.PENDING)
                .newStatus(SupportRequestStatus.UNDER_REVIEW)
                .updateMessage("Application is under review.")
                .createdAt(LocalDateTime.now())
                .build();

        when(supportTrackingService.getRequestUpdates(eq(1L), eq(testUser)))
                .thenReturn(List.of(updateDto));

        mockMvc.perform(get("/api/support-requests/1/updates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(5))
                .andExpect(jsonPath("$.data[0].newStatus").value("UNDER_REVIEW"));
    }

    @Test
    @DisplayName("POST /api/support-requests/{id}/additional-info submits additional info")
    void testSubmitAdditionalInfo_Success() throws Exception {
        com.rememberme.api.communityfund.dto.request.SubmitAdditionalInfoDto dto =
                com.rememberme.api.communityfund.dto.request.SubmitAdditionalInfoDto.builder()
                        .message("Please find the additional information requested.")
                        .documentIds(List.of(3L, 4L))
                        .build();

        SupportRequestResponseDto responseDto = SupportRequestResponseDto.builder()
                .id(1L)
                .status(SupportRequestStatus.UNDER_REVIEW)
                .build();

        when(supportTrackingService.submitAdditionalInfo(eq(1L), any(), eq(testUser)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/support-requests/1/additional-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Additional information submitted successfully"))
                .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));
    }

    @Test
    @DisplayName("GET /api/support-requests/{id}/history returns paginated status history")
    void testGetRequestHistory_Success() throws Exception {
        SupportRequestUpdateResponseDto updateDto = SupportRequestUpdateResponseDto.builder()
                .id(5L)
                .supportRequestId(1L)
                .previousStatus(SupportRequestStatus.PENDING)
                .newStatus(SupportRequestStatus.UNDER_REVIEW)
                .updateMessage("Application is under review.")
                .createdAt(LocalDateTime.now())
                .build();

        org.springframework.data.domain.Page<SupportRequestUpdateResponseDto> page =
                new org.springframework.data.domain.PageImpl<>(List.of(updateDto));

        when(supportTrackingService.getRequestHistory(eq(1L), eq(testUser), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/support-requests/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(5));
    }
}
