package com.rememberme.api.communityfund.service;

import com.rememberme.api.communityfund.dto.request.SubmitAdditionalInfoDto;
import com.rememberme.api.communityfund.dto.response.SupportRequestResponseDto;
import com.rememberme.api.communityfund.dto.response.SupportRequestStatusResponseDto;
import com.rememberme.api.communityfund.dto.response.SupportRequestSummaryDto;
import com.rememberme.api.communityfund.dto.response.SupportRequestUpdateResponseDto;
import com.rememberme.api.communityfund.entity.SupportDocument;
import com.rememberme.api.communityfund.entity.SupportInformationRequest;
import com.rememberme.api.communityfund.entity.SupportRequest;
import com.rememberme.api.communityfund.entity.SupportRequestUpdate;
import com.rememberme.api.communityfund.enums.SupportDocumentType;
import com.rememberme.api.communityfund.enums.SupportRequestStatus;
import com.rememberme.api.communityfund.enums.SupportUrgency;
import com.rememberme.api.communityfund.repository.SupportDocumentRepository;
import com.rememberme.api.communityfund.repository.SupportInformationRequestRepository;
import com.rememberme.api.communityfund.repository.SupportRequestRepository;
import com.rememberme.api.communityfund.repository.SupportRequestUpdateRepository;
import com.rememberme.api.communityfund.security.CommunityFundSecurityHelper;
import com.rememberme.api.communityfund.service.impl.SupportTrackingServiceImpl;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SupportTrackingServiceTest {

    @Mock
    private SupportRequestRepository supportRequestRepository;

    @Mock
    private SupportRequestUpdateRepository updateRepository;

    @Mock
    private SupportInformationRequestRepository infoRequestRepository;

    @Mock
    private SupportDocumentRepository documentRepository;

    @Mock
    private CommunityFundSecurityHelper securityHelper;

    @InjectMocks
    private SupportTrackingServiceImpl trackingService;

    private User applicant;
    private User otherApplicant;
    private User admin;
    private SupportRequest request;

    @BeforeEach
    void setUp() {
        applicant = User.builder().id(1L).email("applicant@example.com").role(User.Role.USER).build();
        otherApplicant = User.builder().id(2L).email("stranger@example.com").role(User.Role.USER).build();
        admin = User.builder().id(99L).email("admin@rememberme.com").role(User.Role.ADMIN).build();

        request = SupportRequest.builder()
                .id(10L)
                .applicant(applicant)
                .applicantName("Ahmed Khan")
                .email("applicant@example.com")
                .phone("+447700900123")
                .relationshipToDeceased("Son")
                .burialProviderName("Funeral Services")
                .burialProviderContact("+447700900456")
                .totalCost(new BigDecimal("3000.00"))
                .otherFunding(new BigDecimal("1000.00"))
                .requestedAmount(new BigDecimal("2000.00"))
                .currency("GBP")
                .urgency(SupportUrgency.HIGH)
                .status(SupportRequestStatus.UNDER_REVIEW)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Applicant can retrieve their own applications")
    void testGetMyApplications_Success() {
        Page<SupportRequest> page = new PageImpl<>(List.of(request));
        when(supportRequestRepository.findByApplicantIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        Page<SupportRequestSummaryDto> result = trackingService.getMyApplications(applicant, 0, 10, "createdAt", "DESC");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        SupportRequestSummaryDto summary = result.getContent().get(0);
        assertEquals(10L, summary.getId());
        assertEquals(new BigDecimal("2000.00"), summary.getRequestedAmount());
        assertEquals(SupportRequestStatus.UNDER_REVIEW, summary.getStatus());
    }

    @Test
    @DisplayName("Applicant cannot view another user's request status")
    void testGetRequestStatus_UnauthorizedUser_ThrowsForbidden() {
        when(supportRequestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(securityHelper.isAdminOrSuperAdmin(otherApplicant)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () ->
                trackingService.getRequestStatus(10L, otherApplicant));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("Get status returns contextual message for MORE_INFO_REQUIRED")
    void testGetRequestStatus_MoreInfoRequired_ReturnsSpecificMessage() {
        request.setStatus(SupportRequestStatus.MORE_INFO_REQUIRED);
        when(supportRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        SupportInformationRequest info = SupportInformationRequest.builder()
                .id(1L)
                .supportRequest(request)
                .informationRequired("Additional invoice details are required.")
                .status("PENDING")
                .build();
        when(infoRequestRepository.findFirstBySupportRequestIdAndStatusOrderByRequestedAtDesc(10L, "PENDING"))
                .thenReturn(Optional.of(info));

        SupportRequestStatusResponseDto statusDto = trackingService.getRequestStatus(10L, applicant);

        assertNotNull(statusDto);
        assertEquals(10L, statusDto.getRequestId());
        assertEquals(SupportRequestStatus.MORE_INFO_REQUIRED, statusDto.getStatus());
        assertEquals("Additional invoice details are required.", statusDto.getMessage());
    }

    @Test
    @DisplayName("Submit additional info successfully transitions status from MORE_INFO_REQUIRED to UNDER_REVIEW")
    void testSubmitAdditionalInfo_TransitionsStatus() {
        request.setStatus(SupportRequestStatus.MORE_INFO_REQUIRED);
        when(supportRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        SupportDocument validDoc = SupportDocument.builder()
                .id(3L)
                .supportRequest(request)
                .documentType(SupportDocumentType.INVOICE)
                .originalFilename("itemized_invoice.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .build();
        when(documentRepository.findByIdAndSupportRequestId(3L, 10L)).thenReturn(Optional.of(validDoc));

        SupportInformationRequest pendingInfo = SupportInformationRequest.builder()
                .id(1L)
                .supportRequest(request)
                .informationRequired("Please upload itemized invoice.")
                .status("PENDING")
                .build();
        when(infoRequestRepository.findFirstBySupportRequestIdAndStatusOrderByRequestedAtDesc(10L, "PENDING"))
                .thenReturn(Optional.of(pendingInfo));

        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(i -> i.getArgument(0));

        SubmitAdditionalInfoDto submitDto = SubmitAdditionalInfoDto.builder()
                .message("Please find the itemized invoice attached.")
                .documentIds(List.of(3L))
                .build();

        SupportRequestResponseDto response = trackingService.submitAdditionalInfo(10L, submitDto, applicant);

        assertNotNull(response);
        assertEquals(SupportRequestStatus.UNDER_REVIEW, request.getStatus());
        assertEquals("RESPONDED", pendingInfo.getStatus());
        verify(updateRepository, times(1)).save(any(SupportRequestUpdate.class));
    }

    @Test
    @DisplayName("Submit additional info rejects document ID that does not belong to request")
    void testSubmitAdditionalInfo_ForeignDocumentId_ThrowsBadRequest() {
        when(supportRequestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(documentRepository.findByIdAndSupportRequestId(999L, 10L)).thenReturn(Optional.empty());

        SubmitAdditionalInfoDto submitDto = SubmitAdditionalInfoDto.builder()
                .message("Here is the document.")
                .documentIds(List.of(999L))
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                trackingService.submitAdditionalInfo(10L, submitDto, applicant));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("does not belong to support request"));
    }

    @Test
    @DisplayName("Get updates returns chronological application updates")
    void testGetRequestUpdates_Success() {
        when(supportRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        SupportRequestUpdate update = SupportRequestUpdate.builder()
                .id(1L)
                .supportRequest(request)
                .previousStatus(SupportRequestStatus.PENDING)
                .newStatus(SupportRequestStatus.UNDER_REVIEW)
                .updateMessage("Application is under review.")
                .createdAt(LocalDateTime.now())
                .build();
        when(updateRepository.findBySupportRequestIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(update));

        List<SupportRequestUpdateResponseDto> updates = trackingService.getRequestUpdates(10L, applicant);

        assertNotNull(updates);
        assertEquals(1, updates.size());
        assertEquals(SupportRequestStatus.UNDER_REVIEW, updates.get(0).getNewStatus());
    }
}
