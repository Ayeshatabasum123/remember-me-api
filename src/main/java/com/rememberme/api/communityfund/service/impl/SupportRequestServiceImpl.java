package com.rememberme.api.communityfund.service.impl;

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
import com.rememberme.api.communityfund.service.PrivateDocumentStorageService;
import com.rememberme.api.communityfund.service.SupportRequestService;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportRequestServiceImpl implements SupportRequestService {

    private final SupportRequestRepository supportRequestRepository;
    private final SupportDocumentRepository supportDocumentRepository;
    private final PrivateDocumentStorageService documentStorageService;
    private final CommunityFundSecurityHelper securityHelper;

    @Override
    @Transactional
    public SupportRequestCreateResponseDto createSupportRequest(User applicant, CreateSupportRequestDto dto, String idempotencyKey) {
        if (applicant == null) {
            throw new ApiException("Authentication is required to submit a support request", HttpStatus.UNAUTHORIZED);
        }

        // Idempotency check if key provided
        if (StringUtils.hasText(idempotencyKey)) {
            var existing = supportRequestRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                SupportRequest existingReq = existing.get();
                if (existingReq.getApplicant().getId().equals(applicant.getId())) {
                    log.info("Idempotent support request replay for key: {}, returning request ID: {}", idempotencyKey, existingReq.getId());
                    return SupportRequestCreateResponseDto.builder()
                            .id(existingReq.getId())
                            .status(existingReq.getStatus())
                            .requestedAmount(existingReq.getRequestedAmount())
                            .currency(existingReq.getCurrency())
                            .build();
                } else {
                    throw new ApiException("Idempotency key has already been used by another account", HttpStatus.CONFLICT);
                }
            }
        }

        // Validation Rules:
        if (!Boolean.TRUE.equals(dto.getAuthorityToApply())) {
            throw new ApiException("Applicant must confirm authority to apply on behalf of the deceased and family", HttpStatus.BAD_REQUEST);
        }

        if (dto.getTotalCost() == null || dto.getTotalCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Total burial cost must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        BigDecimal otherFunding = dto.getOtherFunding() != null ? dto.getOtherFunding() : BigDecimal.ZERO;
        if (otherFunding.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException("Other funding cannot be negative", HttpStatus.BAD_REQUEST);
        }

        if (dto.getRequestedAmount() == null || dto.getRequestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Requested amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        BigDecimal eligibleOutstandingCost = dto.getTotalCost().subtract(otherFunding);
        if (eligibleOutstandingCost.compareTo(BigDecimal.ZERO) < 0) {
            eligibleOutstandingCost = BigDecimal.ZERO;
        }

        if (dto.getRequestedAmount().compareTo(eligibleOutstandingCost) > 0) {
            throw new ApiException(
                    String.format("Requested amount (£%.2f) cannot exceed eligible outstanding burial costs (£%.2f = Total Cost £%.2f - Other Funding £%.2f)",
                            dto.getRequestedAmount(), eligibleOutstandingCost, dto.getTotalCost(), otherFunding),
                    HttpStatus.BAD_REQUEST
            );
        }

        String currency = dto.getCurrency() != null ? dto.getCurrency().trim().toUpperCase() : "GBP";
        if (!"GBP".equals(currency)) {
            throw new ApiException("Currency must be GBP for the initial release", HttpStatus.BAD_REQUEST);
        }

        SupportUrgency urgency = dto.getUrgency() != null ? dto.getUrgency() : SupportUrgency.MEDIUM;

        SupportRequest request = SupportRequest.builder()
                .applicant(applicant)
                .applicantName(dto.getApplicantName().trim())
                .email(dto.getEmail().trim().toLowerCase())
                .phone(dto.getPhone().trim())
                .relationshipToDeceased(dto.getRelationshipToDeceased().trim())
                .authorityToApply(true)
                .burialProviderName(dto.getBurialProviderName().trim())
                .burialProviderContact(dto.getBurialProviderContact().trim())
                .totalCost(dto.getTotalCost())
                .otherFunding(otherFunding)
                .requestedAmount(dto.getRequestedAmount())
                .currency("GBP")
                .urgency(urgency)
                .supportingNotes(dto.getSupportingNotes())
                .status(SupportRequestStatus.PENDING)
                .idempotencyKey(StringUtils.hasText(idempotencyKey) ? idempotencyKey.trim() : null)
                .build();

        SupportRequest saved = supportRequestRepository.save(request);
        log.info("Created support request ID: {} for applicant user ID: {} ({}) with requested amount: £{}",
                saved.getId(), applicant.getId(), applicant.getEmail(), saved.getRequestedAmount());

        return SupportRequestCreateResponseDto.builder()
                .id(saved.getId())
                .status(saved.getStatus())
                .requestedAmount(saved.getRequestedAmount())
                .currency(saved.getCurrency())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SupportRequestResponseDto getSupportRequest(Long id, User currentUser) {
        SupportRequest request = findRequestAndVerifyAccess(id, currentUser);
        return mapToResponseDto(request);
    }

    @Override
    @Transactional
    public SupportRequestResponseDto updateSupportRequest(Long id, UpdateSupportRequestDto dto, User currentUser) {
        SupportRequest request = findRequestAndVerifyAccess(id, currentUser);

        // Editing permitted only in designated editable states
        if (request.getStatus() != SupportRequestStatus.PENDING && request.getStatus() != SupportRequestStatus.MORE_INFO_REQUIRED) {
            throw new ApiException(
                    String.format("Support request cannot be edited in its current status: %s. Editing is only permitted when PENDING or MORE_INFO_REQUIRED.",
                            request.getStatus()),
                    HttpStatus.BAD_REQUEST
            );
        }

        BigDecimal updatedTotalCost = dto.getTotalCost() != null ? dto.getTotalCost() : request.getTotalCost();
        BigDecimal updatedOtherFunding = dto.getOtherFunding() != null ? dto.getOtherFunding() : request.getOtherFunding();
        BigDecimal updatedRequestedAmount = dto.getRequestedAmount() != null ? dto.getRequestedAmount() : request.getRequestedAmount();

        if (updatedTotalCost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Total burial cost must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        if (updatedOtherFunding.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException("Other funding cannot be negative", HttpStatus.BAD_REQUEST);
        }

        if (updatedRequestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Requested amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        BigDecimal eligibleOutstandingCost = updatedTotalCost.subtract(updatedOtherFunding);
        if (updatedRequestedAmount.compareTo(eligibleOutstandingCost) > 0) {
            throw new ApiException(
                    String.format("Requested amount (£%.2f) cannot exceed eligible outstanding burial costs (£%.2f)",
                            updatedRequestedAmount, eligibleOutstandingCost),
                    HttpStatus.BAD_REQUEST
            );
        }

        if (StringUtils.hasText(dto.getApplicantName())) {
            request.setApplicantName(dto.getApplicantName().trim());
        }
        if (StringUtils.hasText(dto.getEmail())) {
            request.setEmail(dto.getEmail().trim().toLowerCase());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            request.setPhone(dto.getPhone().trim());
        }
        if (StringUtils.hasText(dto.getRelationshipToDeceased())) {
            request.setRelationshipToDeceased(dto.getRelationshipToDeceased().trim());
        }
        if (StringUtils.hasText(dto.getBurialProviderName())) {
            request.setBurialProviderName(dto.getBurialProviderName().trim());
        }
        if (StringUtils.hasText(dto.getBurialProviderContact())) {
            request.setBurialProviderContact(dto.getBurialProviderContact().trim());
        }
        request.setTotalCost(updatedTotalCost);
        request.setOtherFunding(updatedOtherFunding);
        request.setRequestedAmount(updatedRequestedAmount);

        if (dto.getUrgency() != null) {
            request.setUrgency(dto.getUrgency());
        }
        if (dto.getSupportingNotes() != null) {
            request.setSupportingNotes(dto.getSupportingNotes());
        }

        SupportRequest updated = supportRequestRepository.save(request);
        log.info("Updated support request ID: {} by user ID: {}", updated.getId(), currentUser.getId());
        return mapToResponseDto(updated);
    }

    @Override
    @Transactional
    public SupportDocumentResponseDto uploadDocument(Long requestId, MultipartFile file, SupportDocumentType documentType, User currentUser) {
        SupportRequest request = findRequestAndVerifyAccess(requestId, currentUser);

        // Check if editing or uploading is permitted
        boolean isStaff = securityHelper.isAdminOrSuperAdmin(currentUser);
        if (!isStaff && request.getStatus() != SupportRequestStatus.PENDING && request.getStatus() != SupportRequestStatus.MORE_INFO_REQUIRED) {
            throw new ApiException(
                    String.format("Documents cannot be uploaded when application is in status: %s", request.getStatus()),
                    HttpStatus.BAD_REQUEST
            );
        }

        if (documentType == null) {
            documentType = SupportDocumentType.SUPPORTING_DOCUMENT;
        }

        // Calculate hash first to check duplicate invoice rule
        String fileHash = documentStorageService.calculateSha256(file);

        // Duplicate invoice detection: prevent duplicate invoices on this request
        if (documentType == SupportDocumentType.INVOICE || documentType == SupportDocumentType.FUNERAL_DIRECTOR_INVOICE) {
            boolean duplicateExists = supportDocumentRepository.existsBySupportRequestIdAndDocumentTypeAndFileHash(requestId, documentType, fileHash);
            if (duplicateExists) {
                throw new ApiException("Duplicate invoice detected. A document with identical contents has already been uploaded for this request", HttpStatus.BAD_REQUEST);
            }
        }

        PrivateDocumentStorageService.StoredDocumentInfo storedInfo = documentStorageService.storePrivateDocument(file);

        SupportDocument document = SupportDocument.builder()
                .supportRequest(request)
                .documentType(documentType)
                .originalFilename(storedInfo.getOriginalFilename())
                .storageReference(storedInfo.getStorageReference())
                .contentType(storedInfo.getContentType())
                .fileSize(storedInfo.getFileSize())
                .fileHash(storedInfo.getFileHash())
                .uploadedBy(currentUser)
                .build();

        SupportDocument saved = supportDocumentRepository.save(document);
        log.info("Uploaded document ID: {} (type: {}) for support request ID: {} by user ID: {}",
                saved.getId(), saved.getDocumentType(), requestId, currentUser.getId());

        return mapDocumentToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportDocumentResponseDto> listDocuments(Long requestId, User currentUser) {
        findRequestAndVerifyAccess(requestId, currentUser);
        List<SupportDocument> documents = supportDocumentRepository.findBySupportRequestIdOrderByUploadedAtAsc(requestId);
        return documents.stream().map(this::mapDocumentToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadedDocument downloadDocument(Long requestId, Long documentId, User currentUser) {
        findRequestAndVerifyAccess(requestId, currentUser);

        SupportDocument doc = supportDocumentRepository.findByIdAndSupportRequestId(documentId, requestId)
                .orElseThrow(() -> new ApiException("Document not found for this support request", HttpStatus.NOT_FOUND));

        Resource resource = documentStorageService.loadAsResource(doc.getStorageReference());
        return new DownloadedDocument(resource, doc.getOriginalFilename(), doc.getContentType(), doc.getFileSize());
    }

    @Override
    @Transactional
    public void deleteDocument(Long requestId, Long documentId, User currentUser) {
        SupportRequest request = findRequestAndVerifyAccess(requestId, currentUser);

        boolean isStaff = securityHelper.isAdminOrSuperAdmin(currentUser);
        if (!isStaff && request.getStatus() != SupportRequestStatus.PENDING && request.getStatus() != SupportRequestStatus.MORE_INFO_REQUIRED) {
            throw new ApiException(
                    String.format("Documents cannot be deleted when application is in status: %s", request.getStatus()),
                    HttpStatus.BAD_REQUEST
            );
        }

        SupportDocument doc = supportDocumentRepository.findByIdAndSupportRequestId(documentId, requestId)
                .orElseThrow(() -> new ApiException("Document not found for this support request", HttpStatus.NOT_FOUND));

        documentStorageService.deleteDocumentFile(doc.getStorageReference());
        supportDocumentRepository.delete(doc);
        log.info("Deleted document ID: {} from support request ID: {} by user ID: {}", documentId, requestId, currentUser.getId());
    }

    private SupportRequest findRequestAndVerifyAccess(Long requestId, User currentUser) {
        if (currentUser == null) {
            throw new ApiException("Authentication required to access support request", HttpStatus.UNAUTHORIZED);
        }

        SupportRequest request = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException("Support request not found with ID: " + requestId, HttpStatus.NOT_FOUND));

        boolean isOwner = request.getApplicant() != null && request.getApplicant().getId().equals(currentUser.getId());
        boolean isStaff = securityHelper.isAdminOrSuperAdmin(currentUser);

        if (!isOwner && !isStaff) {
            log.warn("Access Denied: User ID {} ({}) attempted to access support request ID {} belonging to user ID {}",
                    currentUser.getId(), currentUser.getEmail(), requestId,
                    request.getApplicant() != null ? request.getApplicant().getId() : "null");
            throw new ApiException("Access Denied: You do not have permission to view or manage this support request", HttpStatus.FORBIDDEN);
        }

        return request;
    }

    private SupportRequestResponseDto mapToResponseDto(SupportRequest request) {
        List<SupportDocument> docs = supportDocumentRepository.findBySupportRequestIdOrderByUploadedAtAsc(request.getId());
        List<SupportDocumentResponseDto> docDtos = docs.stream().map(this::mapDocumentToDto).collect(Collectors.toList());

        BigDecimal otherFunding = request.getOtherFunding() != null ? request.getOtherFunding() : BigDecimal.ZERO;
        BigDecimal eligible = request.getTotalCost().subtract(otherFunding);
        if (eligible.compareTo(BigDecimal.ZERO) < 0) {
            eligible = BigDecimal.ZERO;
        }

        return SupportRequestResponseDto.builder()
                .id(request.getId())
                .applicantUserId(request.getApplicant() != null ? request.getApplicant().getId() : null)
                .applicantName(request.getApplicantName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .relationshipToDeceased(request.getRelationshipToDeceased())
                .authorityToApply(request.getAuthorityToApply())
                .burialProviderName(request.getBurialProviderName())
                .burialProviderContact(request.getBurialProviderContact())
                .totalCost(request.getTotalCost())
                .otherFunding(otherFunding)
                .eligibleOutstandingCost(eligible)
                .requestedAmount(request.getRequestedAmount())
                .currency(request.getCurrency())
                .urgency(request.getUrgency())
                .supportingNotes(request.getSupportingNotes())
                .status(request.getStatus())
                .providerVerified(request.getProviderVerified())
                .documents(docDtos)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    private SupportDocumentResponseDto mapDocumentToDto(SupportDocument doc) {
        return SupportDocumentResponseDto.builder()
                .id(doc.getId())
                .supportRequestId(doc.getSupportRequest() != null ? doc.getSupportRequest().getId() : null)
                .documentType(doc.getDocumentType())
                .originalFilename(doc.getOriginalFilename())
                .contentType(doc.getContentType())
                .fileSize(doc.getFileSize())
                .uploadedByUserId(doc.getUploadedBy() != null ? doc.getUploadedBy().getId() : null)
                .uploadedByEmail(doc.getUploadedBy() != null ? doc.getUploadedBy().getEmail() : null)
                .uploadedAt(doc.getUploadedAt())
                .build();
    }
}
