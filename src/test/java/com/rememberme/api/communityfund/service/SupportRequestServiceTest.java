package com.rememberme.api.communityfund.service;

import com.rememberme.api.communityfund.dto.request.CreateSupportRequestDto;
import com.rememberme.api.communityfund.dto.request.UpdateSupportRequestDto;
import com.rememberme.api.communityfund.dto.response.SupportDocumentResponseDto;
import com.rememberme.api.communityfund.dto.response.SupportRequestCreateResponseDto;
import com.rememberme.api.communityfund.dto.response.SupportRequestResponseDto;
import com.rememberme.api.communityfund.entity.SupportDocument;
import com.rememberme.api.communityfund.entity.SupportRequest;
import com.rememberme.api.communityfund.enums.SupportDocumentType;
import com.rememberme.api.communityfund.enums.SupportRequestStatus;
import com.rememberme.api.communityfund.enums.SupportUrgency;
import com.rememberme.api.communityfund.repository.SupportDocumentRepository;
import com.rememberme.api.communityfund.repository.SupportRequestRepository;
import com.rememberme.api.communityfund.security.CommunityFundSecurityHelper;
import com.rememberme.api.communityfund.service.impl.SupportRequestServiceImpl;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SupportRequestServiceTest {

    @Mock
    private SupportRequestRepository supportRequestRepository;

    @Mock
    private SupportDocumentRepository supportDocumentRepository;

    @Mock
    private PrivateDocumentStorageService documentStorageService;

    @Mock
    private CommunityFundSecurityHelper securityHelper;

    @InjectMocks
    private SupportRequestServiceImpl supportRequestService;

    private User applicantUser;
    private User otherUser;
    private User adminUser;
    private CreateSupportRequestDto validCreateDto;

    @BeforeEach
    void setUp() {
        applicantUser = User.builder()
                .id(1L)
                .email("ahmed@example.com")
                .fullName("Ahmed Khan")
                .role(User.Role.USER)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("stranger@example.com")
                .fullName("Stranger")
                .role(User.Role.USER)
                .build();

        adminUser = User.builder()
                .id(99L)
                .email("admin@rememberme.com")
                .fullName("Admin Officer")
                .role(User.Role.ADMIN)
                .build();

        validCreateDto = CreateSupportRequestDto.builder()
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
    }

    @Test
    @DisplayName("Create support request succeeds with valid data")
    void testCreateSupportRequest_Success() {
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(invocation -> {
            SupportRequest r = invocation.getArgument(0);
            r.setId(10L);
            r.setCreatedAt(LocalDateTime.now());
            r.setUpdatedAt(LocalDateTime.now());
            return r;
        });

        SupportRequestCreateResponseDto result = supportRequestService.createSupportRequest(applicantUser, validCreateDto, null);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(SupportRequestStatus.PENDING, result.getStatus());
        assertEquals(new BigDecimal("2000.00"), result.getRequestedAmount());
        assertEquals("GBP", result.getCurrency());
        verify(supportRequestRepository, times(1)).save(any(SupportRequest.class));
    }

    @Test
    @DisplayName("Create support request fails when authorityToApply is false")
    void testCreateSupportRequest_AuthorityNotConfirmed_ThrowsException() {
        validCreateDto.setAuthorityToApply(false);

        ApiException ex = assertThrows(ApiException.class, () ->
                supportRequestService.createSupportRequest(applicantUser, validCreateDto, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("authority to apply"));
    }

    @Test
    @DisplayName("Create support request fails when otherFunding is negative")
    void testCreateSupportRequest_NegativeOtherFunding_ThrowsException() {
        validCreateDto.setOtherFunding(new BigDecimal("-50.00"));

        ApiException ex = assertThrows(ApiException.class, () ->
                supportRequestService.createSupportRequest(applicantUser, validCreateDto, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Other funding cannot be negative"));
    }

    @Test
    @DisplayName("Create support request fails when requestedAmount exceeds eligible outstanding costs")
    void testCreateSupportRequest_ExceedingRequestedAmount_ThrowsException() {
        // total 3000, other 1000 => outstanding 2000. Requested 2500 => invalid!
        validCreateDto.setRequestedAmount(new BigDecimal("2500.00"));

        ApiException ex = assertThrows(ApiException.class, () ->
                supportRequestService.createSupportRequest(applicantUser, validCreateDto, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("cannot exceed eligible outstanding burial costs"));
    }

    @Test
    @DisplayName("Create support request fails when currency is not GBP")
    void testCreateSupportRequest_InvalidCurrency_ThrowsException() {
        validCreateDto.setCurrency("EUR");

        ApiException ex = assertThrows(ApiException.class, () ->
                supportRequestService.createSupportRequest(applicantUser, validCreateDto, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Currency must be GBP"));
    }

    @Test
    @DisplayName("Create support request is idempotent when duplicate idempotency key is supplied")
    void testCreateSupportRequest_Idempotent() {
        SupportRequest existing = SupportRequest.builder()
                .id(10L)
                .applicant(applicantUser)
                .status(SupportRequestStatus.PENDING)
                .requestedAmount(new BigDecimal("2000.00"))
                .currency("GBP")
                .idempotencyKey("KEY-12345")
                .build();

        when(supportRequestRepository.findByIdempotencyKey("KEY-12345")).thenReturn(Optional.of(existing));

        SupportRequestCreateResponseDto result = supportRequestService.createSupportRequest(applicantUser, validCreateDto, "KEY-12345");

        assertNotNull(result);
        assertEquals(10L, result.getId());
        verify(supportRequestRepository, never()).save(any(SupportRequest.class));
    }

    @Test
    @DisplayName("Get support request succeeds for applicant owner")
    void testGetSupportRequest_Owner_Success() {
        SupportRequest request = SupportRequest.builder()
                .id(1L)
                .applicant(applicantUser)
                .applicantName("Ahmed Khan")
                .email("ahmed@example.com")
                .phone("+447700900123")
                .relationshipToDeceased("Son")
                .burialProviderName("Example Funeral Services")
                .burialProviderContact("+447700900456")
                .totalCost(new BigDecimal("3000.00"))
                .otherFunding(new BigDecimal("1000.00"))
                .requestedAmount(new BigDecimal("2000.00"))
                .currency("GBP")
                .urgency(SupportUrgency.HIGH)
                .status(SupportRequestStatus.PENDING)
                .documents(new ArrayList<>())
                .build();

        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(supportDocumentRepository.findBySupportRequestIdOrderByUploadedAtAsc(1L)).thenReturn(List.of());

        SupportRequestResponseDto response = supportRequestService.getSupportRequest(1L, applicantUser);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Ahmed Khan", response.getApplicantName());
        assertEquals(new BigDecimal("2000.00"), response.getEligibleOutstandingCost());
    }

    @Test
    @DisplayName("Get support request fails for unauthorized user")
    void testGetSupportRequest_UnauthorizedUser_ThrowsForbidden() {
        SupportRequest request = SupportRequest.builder()
                .id(1L)
                .applicant(applicantUser)
                .status(SupportRequestStatus.PENDING)
                .build();

        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(securityHelper.isAdminOrSuperAdmin(otherUser)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () ->
                supportRequestService.getSupportRequest(1L, otherUser));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("Get support request succeeds for administrator")
    void testGetSupportRequest_Admin_Success() {
        SupportRequest request = SupportRequest.builder()
                .id(1L)
                .applicant(applicantUser)
                .totalCost(new BigDecimal("3000.00"))
                .otherFunding(BigDecimal.ZERO)
                .requestedAmount(new BigDecimal("2000.00"))
                .status(SupportRequestStatus.PENDING)
                .build();

        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(securityHelper.isAdminOrSuperAdmin(adminUser)).thenReturn(true);
        when(supportDocumentRepository.findBySupportRequestIdOrderByUploadedAtAsc(1L)).thenReturn(List.of());

        SupportRequestResponseDto response = supportRequestService.getSupportRequest(1L, adminUser);
        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("Update support request succeeds in PENDING status")
    void testUpdateSupportRequest_PendingState_Success() {
        SupportRequest request = SupportRequest.builder()
                .id(1L)
                .applicant(applicantUser)
                .applicantName("Ahmed Khan")
                .email("ahmed@example.com")
                .phone("+447700900123")
                .relationshipToDeceased("Son")
                .burialProviderName("Example Funeral Services")
                .burialProviderContact("+447700900456")
                .totalCost(new BigDecimal("3000.00"))
                .otherFunding(new BigDecimal("1000.00"))
                .requestedAmount(new BigDecimal("2000.00"))
                .currency("GBP")
                .urgency(SupportUrgency.MEDIUM)
                .status(SupportRequestStatus.PENDING)
                .build();

        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(i -> i.getArgument(0));

        UpdateSupportRequestDto updateDto = UpdateSupportRequestDto.builder()
                .applicantName("Ahmed K. Khan")
                .urgency(SupportUrgency.HIGH)
                .build();

        SupportRequestResponseDto updated = supportRequestService.updateSupportRequest(1L, updateDto, applicantUser);

        assertNotNull(updated);
        assertEquals("Ahmed K. Khan", updated.getApplicantName());
        assertEquals(SupportUrgency.HIGH, updated.getUrgency());
    }

    @Test
    @DisplayName("Update support request fails in non-editable status (APPROVED)")
    void testUpdateSupportRequest_NonEditableState_ThrowsBadRequest() {
        SupportRequest request = SupportRequest.builder()
                .id(1L)
                .applicant(applicantUser)
                .status(SupportRequestStatus.APPROVED)
                .build();

        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        UpdateSupportRequestDto updateDto = UpdateSupportRequestDto.builder()
                .applicantName("Ahmed K. Khan")
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                supportRequestService.updateSupportRequest(1L, updateDto, applicantUser));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("cannot be edited"));
    }

    @Test
    @DisplayName("Upload document succeeds for valid PDF")
    void testUploadDocument_Success() {
        SupportRequest request = SupportRequest.builder()
                .id(1L)
                .applicant(applicantUser)
                .status(SupportRequestStatus.PENDING)
                .build();

        byte[] pdfBytes = "%PDF-1.4 mock content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "invoice.pdf", "application/pdf", pdfBytes);

        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(documentStorageService.calculateSha256(file)).thenReturn("dummy_hash_123");
        when(supportDocumentRepository.existsBySupportRequestIdAndDocumentTypeAndFileHash(1L, SupportDocumentType.INVOICE, "dummy_hash_123")).thenReturn(false);

        PrivateDocumentStorageService.StoredDocumentInfo storedInfo =
                new PrivateDocumentStorageService.StoredDocumentInfo("/uploads/private_documents/uuid.pdf", "invoice.pdf", "application/pdf", pdfBytes.length, "dummy_hash_123");
        when(documentStorageService.storePrivateDocument(file)).thenReturn(storedInfo);

        when(supportDocumentRepository.save(any(SupportDocument.class))).thenAnswer(i -> {
            SupportDocument doc = i.getArgument(0);
            doc.setId(101L);
            doc.setUploadedAt(LocalDateTime.now());
            return doc;
        });

        SupportDocumentResponseDto response = supportRequestService.uploadDocument(1L, file, SupportDocumentType.INVOICE, applicantUser);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals(SupportDocumentType.INVOICE, response.getDocumentType());
        assertEquals("invoice.pdf", response.getOriginalFilename());
    }

    @Test
    @DisplayName("Upload document fails when duplicate invoice is detected")
    void testUploadDocument_DuplicateInvoice_ThrowsBadRequest() {
        SupportRequest request = SupportRequest.builder()
                .id(1L)
                .applicant(applicantUser)
                .status(SupportRequestStatus.PENDING)
                .build();

        byte[] pdfBytes = "%PDF-1.4 duplicate".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "duplicate_invoice.pdf", "application/pdf", pdfBytes);

        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(documentStorageService.calculateSha256(file)).thenReturn("identical_hash");
        when(supportDocumentRepository.existsBySupportRequestIdAndDocumentTypeAndFileHash(1L, SupportDocumentType.INVOICE, "identical_hash")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () ->
                supportRequestService.uploadDocument(1L, file, SupportDocumentType.INVOICE, applicantUser));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Duplicate invoice detected"));
    }

    @Test
    @DisplayName("Secure document download streams file as resource")
    void testDownloadDocument_Success() {
        SupportRequest request = SupportRequest.builder()
                .id(1L)
                .applicant(applicantUser)
                .status(SupportRequestStatus.PENDING)
                .build();

        SupportDocument doc = SupportDocument.builder()
                .id(20L)
                .supportRequest(request)
                .originalFilename("invoice.pdf")
                .storageReference("/uploads/private_documents/inv.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .build();

        Resource mockResource = new ByteArrayResource("PDF content".getBytes());

        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(supportDocumentRepository.findByIdAndSupportRequestId(20L, 1L)).thenReturn(Optional.of(doc));
        when(documentStorageService.loadAsResource("/uploads/private_documents/inv.pdf")).thenReturn(mockResource);

        SupportRequestService.DownloadedDocument result = supportRequestService.downloadDocument(1L, 20L, applicantUser);

        assertNotNull(result);
        assertEquals("invoice.pdf", result.originalFilename());
        assertEquals("application/pdf", result.contentType());
    }

    @Test
    @DisplayName("Delete document fails when request is in PAID status")
    void testDeleteDocument_NonEditableStatus_ThrowsBadRequest() {
        SupportRequest request = SupportRequest.builder()
                .id(1L)
                .applicant(applicantUser)
                .status(SupportRequestStatus.PAID)
                .build();

        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(securityHelper.isAdminOrSuperAdmin(applicantUser)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () ->
                supportRequestService.deleteDocument(1L, 20L, applicantUser));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Documents cannot be deleted"));
    }
}
