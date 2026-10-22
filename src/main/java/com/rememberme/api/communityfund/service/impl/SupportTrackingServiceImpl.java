package com.rememberme.api.communityfund.service.impl;

import com.rememberme.api.communityfund.dto.request.SubmitAdditionalInfoDto;
import com.rememberme.api.communityfund.dto.response.SupportDocumentResponseDto;
import com.rememberme.api.communityfund.dto.response.SupportRequestResponseDto;
import com.rememberme.api.communityfund.dto.response.SupportRequestStatusResponseDto;
import com.rememberme.api.communityfund.dto.response.SupportRequestSummaryDto;
import com.rememberme.api.communityfund.dto.response.SupportRequestUpdateResponseDto;
import com.rememberme.api.communityfund.entity.SupportDocument;
import com.rememberme.api.communityfund.entity.SupportInformationRequest;
import com.rememberme.api.communityfund.entity.SupportRequest;
import com.rememberme.api.communityfund.entity.SupportRequestUpdate;
import com.rememberme.api.communityfund.enums.SupportRequestStatus;
import com.rememberme.api.communityfund.repository.SupportDocumentRepository;
import com.rememberme.api.communityfund.repository.SupportInformationRequestRepository;
import com.rememberme.api.communityfund.repository.SupportRequestRepository;
import com.rememberme.api.communityfund.repository.SupportRequestUpdateRepository;
import com.rememberme.api.communityfund.security.CommunityFundSecurityHelper;
import com.rememberme.api.communityfund.service.SupportTrackingService;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTrackingServiceImpl implements SupportTrackingService {

    private final SupportRequestRepository supportRequestRepository;
    private final SupportRequestUpdateRepository updateRepository;
    private final SupportInformationRequestRepository infoRequestRepository;
    private final SupportDocumentRepository documentRepository;
    private final CommunityFundSecurityHelper securityHelper;

    @Override
    @Transactional(readOnly = true)
    public Page<SupportRequestSummaryDto> getMyApplications(User applicant, int page, int size, String sortBy, String direction) {
        if (applicant == null) {
            throw new ApiException("Authentication required to view applications", HttpStatus.UNAUTHORIZED);
        }

        int pageSize = Math.max(1, Math.min(size, 100));
        int pageNumber = Math.max(0, page);
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortDirection, sortProperty));
        Page<SupportRequest> requests = supportRequestRepository.findByApplicantIdOrderByCreatedAtDesc(applicant.getId(), pageable);

        return requests.map(req -> SupportRequestSummaryDto.builder()
                .id(req.getId())
                .requestedAmount(req.getRequestedAmount())
                .currency(req.getCurrency())
                .status(req.getStatus())
                .urgency(req.getUrgency())
                .createdAt(req.getCreatedAt())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public SupportRequestStatusResponseDto getRequestStatus(Long requestId, User currentUser) {
        SupportRequest request = findRequestAndVerifyAccess(requestId, currentUser);

        String message = getDefaultStatusMessage(request.getStatus());

        if (request.getStatus() == SupportRequestStatus.MORE_INFO_REQUIRED) {
            Optional<SupportInformationRequest> pendingInfo = infoRequestRepository
                    .findFirstBySupportRequestIdAndStatusOrderByRequestedAtDesc(requestId, "PENDING");
            if (pendingInfo.isPresent() && pendingInfo.get().getInformationRequired() != null) {
                message = pendingInfo.get().getInformationRequired();
            }
        }

        return SupportRequestStatusResponseDto.builder()
                .requestId(request.getId())
                .status(request.getStatus())
                .message(message)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportRequestUpdateResponseDto> getRequestUpdates(Long requestId, User currentUser) {
        findRequestAndVerifyAccess(requestId, currentUser);
        List<SupportRequestUpdate> updates = updateRepository.findBySupportRequestIdOrderByCreatedAtDesc(requestId);

        return updates.stream().map(u -> SupportRequestUpdateResponseDto.builder()
                .id(u.getId())
                .supportRequestId(requestId)
                .previousStatus(u.getPreviousStatus())
                .newStatus(u.getNewStatus())
                .updateMessage(u.getUpdateMessage())
                .createdAt(u.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportRequestUpdateResponseDto> getRequestHistory(Long requestId, User currentUser, int page, int size) {
        findRequestAndVerifyAccess(requestId, currentUser);
        int pageSize = Math.max(1, Math.min(size, 100));
        int pageNumber = Math.max(0, page);

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SupportRequestUpdate> updates = updateRepository.findBySupportRequestIdOrderByCreatedAtDesc(requestId, pageable);

        return updates.map(u -> SupportRequestUpdateResponseDto.builder()
                .id(u.getId())
                .supportRequestId(requestId)
                .previousStatus(u.getPreviousStatus())
                .newStatus(u.getNewStatus())
                .updateMessage(u.getUpdateMessage())
                .createdAt(u.getCreatedAt())
                .build());
    }

    @Override
    @Transactional
    public SupportRequestResponseDto submitAdditionalInfo(Long requestId, SubmitAdditionalInfoDto dto, User currentUser) {
        SupportRequest request = findRequestAndVerifyAccess(requestId, currentUser);

        if (dto == null || dto.getMessage() == null || dto.getMessage().isBlank()) {
            throw new ApiException("Message is required when submitting additional information", HttpStatus.BAD_REQUEST);
        }

        // Verify that each supplied document belongs to this request and is accessible to applicant
        if (dto.getDocumentIds() != null && !dto.getDocumentIds().isEmpty()) {
            for (Long docId : dto.getDocumentIds()) {
                boolean docExists = documentRepository.findByIdAndSupportRequestId(docId, requestId).isPresent();
                if (!docExists) {
                    throw new ApiException(
                            String.format("Supplied document ID %d does not belong to support request ID %d", docId, requestId),
                            HttpStatus.BAD_REQUEST
                    );
                }
            }
        }

        SupportRequestStatus previousStatus = request.getStatus();
        SupportRequestStatus newStatus = previousStatus;

        // If currently in MORE_INFO_REQUIRED status, transition back to UNDER_REVIEW
        if (previousStatus == SupportRequestStatus.MORE_INFO_REQUIRED) {
            newStatus = SupportRequestStatus.UNDER_REVIEW;
            request.setStatus(newStatus);
            request.setUpdatedAt(LocalDateTime.now());
            supportRequestRepository.save(request);

            // Mark any pending information requests as responded
            Optional<SupportInformationRequest> pendingInfo = infoRequestRepository
                    .findFirstBySupportRequestIdAndStatusOrderByRequestedAtDesc(requestId, "PENDING");
            pendingInfo.ifPresent(info -> {
                info.setStatus("RESPONDED");
                info.setRespondedAt(LocalDateTime.now());
                info.setResponseMessage(dto.getMessage().trim());
                infoRequestRepository.save(info);
            });
        }

        // Record update entry in audit trail
        String updateMsg = "Applicant submitted additional information: " + dto.getMessage().trim();
        SupportRequestUpdate update = SupportRequestUpdate.builder()
                .supportRequest(request)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .updateMessage(updateMsg)
                .updatedBy(currentUser)
                .build();
        updateRepository.save(update);

        log.info("Applicant ID {} submitted additional info for request ID {}. Status: {} -> {}",
                currentUser.getId(), requestId, previousStatus, newStatus);

        return mapToResponseDto(request);
    }

    @Override
    @Transactional
    public void recordStatusTransition(Long requestId, SupportRequestStatus previousStatus, SupportRequestStatus newStatus,
                                       String message, User updatedBy) {
        SupportRequest request = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException("Support request not found with ID: " + requestId, HttpStatus.NOT_FOUND));

        SupportRequestUpdate update = SupportRequestUpdate.builder()
                .supportRequest(request)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .updateMessage(message != null ? message : "Status updated from " + previousStatus + " to " + newStatus)
                .updatedBy(updatedBy)
                .build();
        updateRepository.save(update);
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
            throw new ApiException("Access Denied: You do not have permission to view or track this support request", HttpStatus.FORBIDDEN);
        }

        return request;
    }

    private String getDefaultStatusMessage(SupportRequestStatus status) {
        if (status == null) return "Status pending";
        return switch (status) {
            case PENDING -> "Your application has been received and is pending administrative review.";
            case UNDER_REVIEW -> "Your application is currently under administrative review.";
            case MORE_INFO_REQUIRED -> "Additional information or documentation is required to proceed with your application.";
            case APPROVED -> "Your application for financial assistance has been approved by the fund committee.";
            case WAITING_FOR_FUNDS -> "Your application is approved and is queued awaiting communal fund availability.";
            case DECLINED -> "Your application could not be approved at this time.";
            case PAYMENT_PENDING -> "Burial assistance disbursement to the provider is pending execution.";
            case PAID -> "Burial assistance has been successfully disbursed to the registered burial provider.";
        };
    }

    private SupportRequestResponseDto mapToResponseDto(SupportRequest request) {
        List<SupportDocument> docs = documentRepository.findBySupportRequestIdOrderByUploadedAtAsc(request.getId());
        List<SupportDocumentResponseDto> docDtos = docs.stream().map(doc -> SupportDocumentResponseDto.builder()
                .id(doc.getId())
                .supportRequestId(doc.getSupportRequest() != null ? doc.getSupportRequest().getId() : null)
                .documentType(doc.getDocumentType())
                .originalFilename(doc.getOriginalFilename())
                .contentType(doc.getContentType())
                .fileSize(doc.getFileSize())
                .uploadedByUserId(doc.getUploadedBy() != null ? doc.getUploadedBy().getId() : null)
                .uploadedByEmail(doc.getUploadedBy() != null ? doc.getUploadedBy().getEmail() : null)
                .uploadedAt(doc.getUploadedAt())
                .build()).collect(Collectors.toList());

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
}
