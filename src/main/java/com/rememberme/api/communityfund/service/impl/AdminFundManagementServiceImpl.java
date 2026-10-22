package com.rememberme.api.communityfund.service.impl;

import com.rememberme.api.communityfund.dto.request.*;
import com.rememberme.api.communityfund.dto.response.*;
import com.rememberme.api.communityfund.entity.*;
import com.rememberme.api.communityfund.enums.*;
import com.rememberme.api.communityfund.repository.*;
import com.rememberme.api.communityfund.security.CommunityFundSecurityHelper;
import com.rememberme.api.communityfund.service.AdminFundManagementService;
import com.rememberme.api.entity.*;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.AuditLogRepository;
import com.rememberme.api.repository.DonationRepository;
import com.rememberme.api.repository.FundLedgerRepository;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminFundManagementServiceImpl implements AdminFundManagementService {

    private final SupportRequestRepository supportRequestRepository;
    private final SupportDocumentRepository supportDocumentRepository;
    private final SupportRequestUpdateRepository updateRepository;
    private final SupportInformationRequestRepository infoRequestRepository;
    private final SupportApprovalRepository approvalRepository;
    private final FundReservationRepository reservationRepository;
    private final ProviderPaymentRepository providerPaymentRepository;
    private final FundRefundRepository refundRepository;
    private final FundLedgerRepository fundLedgerRepository;
    private final DonationRepository donationRepository;
    private final AuditLogRepository auditLogRepository;
    private final CommunityFundSecurityHelper securityHelper;

    // ==========================================
    // 6.1 Admin Support Request Review & Dashboard
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public Page<SupportRequestResponseDto> getAdminSupportRequests(SupportRequestStatus status, SupportUrgency urgency, String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SupportRequest> requests = supportRequestRepository.findAdminRequestsWithFilters(status, urgency, search, pageable);
        return requests.map(this::mapToResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public SupportRequestResponseDto getAdminSupportRequestDetails(Long id) {
        SupportRequest request = findSupportRequest(id);
        return mapToResponseDto(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportDocumentResponseDto> getAdminSupportRequestDocuments(Long id) {
        findSupportRequest(id);
        List<SupportDocument> docs = supportDocumentRepository.findBySupportRequestIdOrderByUploadedAtAsc(id);
        return docs.stream().map(this::mapDocumentToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SupportRequestResponseDto reviewSupportRequest(Long id, AdminReviewRequestDto dto, User adminUser) {
        verifyAdmin(adminUser);
        SupportRequest request = findSupportRequest(id);

        SupportRequestStatus previousStatus = request.getStatus();
        SupportRequestStatus newStatus = dto.getStatus();

        request.setStatus(newStatus);
        request.setReviewNotes(dto.getReviewNotes());
        request.setReviewedBy(adminUser);
        request.setReviewedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        SupportRequest saved = supportRequestRepository.save(request);

        recordUpdate(saved, previousStatus, newStatus, "Administrative review updated: " + (dto.getReviewNotes() != null ? dto.getReviewNotes() : "Status changed"), adminUser);
        logAudit(adminUser, "SUPPORT_REQUEST_REVIEWED", "SupportRequest", id, "Status updated to " + newStatus);

        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public SupportRequestResponseDto requestAdditionalInfo(Long id, AdminRequestInfoDto dto, User adminUser) {
        verifyAdmin(adminUser);
        SupportRequest request = findSupportRequest(id);

        SupportRequestStatus previousStatus = request.getStatus();
        request.setStatus(SupportRequestStatus.MORE_INFO_REQUIRED);
        request.setUpdatedAt(LocalDateTime.now());
        SupportRequest saved = supportRequestRepository.save(request);

        SupportInformationRequest infoRequest = SupportInformationRequest.builder()
                .supportRequest(saved)
                .requestedBy(adminUser)
                .informationRequired(dto.getMessage().trim())
                .status("PENDING")
                .build();
        infoRequestRepository.save(infoRequest);

        recordUpdate(saved, previousStatus, SupportRequestStatus.MORE_INFO_REQUIRED,
                "Additional information requested by administrator: " + dto.getMessage().trim(), adminUser);
        logAudit(adminUser, "ADDITIONAL_INFO_REQUESTED", "SupportRequest", id, dto.getMessage().trim());

        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public SupportRequestResponseDto verifyProvider(Long id, AdminProviderVerificationDto dto, User adminUser) {
        verifyAdmin(adminUser);
        SupportRequest request = findSupportRequest(id);

        request.setProviderVerified(Boolean.TRUE.equals(dto.getVerified()));
        request.setProviderVerificationNotes(dto.getVerificationNotes());
        request.setUpdatedAt(LocalDateTime.now());
        SupportRequest saved = supportRequestRepository.save(request);

        String note = Boolean.TRUE.equals(dto.getVerified()) ? "Burial provider verified" : "Burial provider verification rejected";
        recordUpdate(saved, saved.getStatus(), saved.getStatus(), note + ": " + dto.getVerificationNotes(), adminUser);
        logAudit(adminUser, "PROVIDER_VERIFIED", "SupportRequest", id, note);

        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public SupportRequestResponseDto declineSupportRequest(Long id, AdminDeclineRequestDto dto, User adminUser) {
        verifyAdmin(adminUser);
        SupportRequest request = findSupportRequest(id);

        SupportRequestStatus previousStatus = request.getStatus();
        request.setStatus(SupportRequestStatus.DECLINED);
        request.setReviewNotes(dto.getReason());
        request.setReviewedBy(adminUser);
        request.setReviewedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        SupportRequest saved = supportRequestRepository.save(request);

        recordUpdate(saved, previousStatus, SupportRequestStatus.DECLINED, "Application declined: " + dto.getReason(), adminUser);
        logAudit(adminUser, "SUPPORT_REQUEST_DECLINED", "SupportRequest", id, "Declined reason: " + dto.getReason());

        return mapToResponseDto(saved);
    }

    // ==========================================
    // 6.2 Two-Person Approval System
    // ==========================================

    @Override
    @Transactional
    public SupportApprovalResponseDto submitApproval(Long requestId, SubmitApprovalDto dto, User approverUser) {
        verifyAdmin(approverUser);
        SupportRequest request = findSupportRequest(requestId);

        // Approver cannot be the applicant
        if (request.getApplicant().getId().equals(approverUser.getId())) {
            throw new ApiException("Applicant cannot approve their own financial support application", HttpStatus.FORBIDDEN);
        }

        // Prevent duplicate approval by same person
        if (approvalRepository.existsBySupportRequestIdAndApproverId(requestId, approverUser.getId())) {
            throw new ApiException("You have already submitted an approval decision for this application", HttpStatus.CONFLICT);
        }

        if (dto.getApprovedAmount() == null || dto.getApprovedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Approved amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        if (dto.getApprovedAmount().compareTo(request.getRequestedAmount()) > 0) {
            throw new ApiException(
                    String.format("Approved amount (£%.2f) cannot exceed requested assistance amount (£%.2f)",
                            dto.getApprovedAmount(), request.getRequestedAmount()),
                    HttpStatus.BAD_REQUEST
            );
        }

        List<SupportApproval> existingApprovals = approvalRepository.findBySupportRequestIdOrderByCreatedAtAsc(requestId);

        // If declining
        if (dto.getDecision() == ApprovalDecision.DECLINED) {
            SupportApproval approval = SupportApproval.builder()
                    .supportRequest(request)
                    .approver(approverUser)
                    .decision(ApprovalDecision.DECLINED)
                    .approvedAmount(BigDecimal.ZERO)
                    .decisionReason(dto.getReason().trim())
                    .build();
            SupportApproval savedApproval = approvalRepository.save(approval);

            SupportRequestStatus prev = request.getStatus();
            request.setStatus(SupportRequestStatus.DECLINED);
            request.setUpdatedAt(LocalDateTime.now());
            supportRequestRepository.save(request);

            recordUpdate(request, prev, SupportRequestStatus.DECLINED, "Application declined during two-person review: " + dto.getReason(), approverUser);
            logAudit(approverUser, "APPROVAL_DECISION_DECLINED", "SupportApproval", savedApproval.getId(), "Declined by " + approverUser.getEmail());

            return mapApprovalToDto(savedApproval);
        }

        // Approving
        if (existingApprovals.isEmpty()) {
            // First approval recorded
            SupportApproval approval = SupportApproval.builder()
                    .supportRequest(request)
                    .approver(approverUser)
                    .decision(ApprovalDecision.APPROVED)
                    .approvedAmount(dto.getApprovedAmount())
                    .decisionReason(dto.getReason().trim())
                    .build();
            SupportApproval savedApproval = approvalRepository.save(approval);

            recordUpdate(request, request.getStatus(), request.getStatus(),
                    String.format("First administrative approval submitted (£%.2f) by %s. Awaiting second authorized approver.",
                            dto.getApprovedAmount(), approverUser.getFullName()), approverUser);
            logAudit(approverUser, "FIRST_APPROVAL_RECORDED", "SupportApproval", savedApproval.getId(),
                    "First approver approved £" + dto.getApprovedAmount());

            return mapApprovalToDto(savedApproval);
        } else {
            // Second approval
            SupportApproval firstApproval = existingApprovals.get(0);
            if (firstApproval.getDecision() == ApprovalDecision.APPROVED) {
                // Amounts must agree
                if (firstApproval.getApprovedAmount().compareTo(dto.getApprovedAmount()) != 0) {
                    throw new ApiException(
                            String.format("Approved amount (£%.2f) does not match the first approver's amount (£%.2f). Both approvers must agree on the final authorized amount.",
                                    dto.getApprovedAmount(), firstApproval.getApprovedAmount()),
                            HttpStatus.BAD_REQUEST
                    );
                }
            }

            SupportApproval approval = SupportApproval.builder()
                    .supportRequest(request)
                    .approver(approverUser)
                    .decision(ApprovalDecision.APPROVED)
                    .approvedAmount(dto.getApprovedAmount())
                    .decisionReason(dto.getReason().trim())
                    .build();
            SupportApproval savedApproval = approvalRepository.save(approval);

            // Two-person approval complete!
            SupportRequestStatus prev = request.getStatus();
            request.setStatus(SupportRequestStatus.APPROVED);
            request.setRequestedAmount(dto.getApprovedAmount()); // Update with agreed amount
            request.setUpdatedAt(LocalDateTime.now());
            supportRequestRepository.save(request);

            recordUpdate(request, prev, SupportRequestStatus.APPROVED,
                    String.format("Two-person administrative approval complete! Authorized assistance: £%.2f.", dto.getApprovedAmount()), approverUser);
            logAudit(approverUser, "TWO_PERSON_APPROVAL_COMPLETED", "SupportRequest", requestId,
                    "Application fully approved for £" + dto.getApprovedAmount());

            return mapApprovalToDto(savedApproval);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportApprovalResponseDto> getRequestApprovals(Long requestId) {
        findSupportRequest(requestId);
        List<SupportApproval> approvals = approvalRepository.findBySupportRequestIdOrderByCreatedAtAsc(requestId);
        return approvals.stream().map(this::mapApprovalToDto).collect(Collectors.toList());
    }

    // ==========================================
    // 6.3 Fund Reservations
    // ==========================================

    @Override
    @Transactional
    public FundReservationResponseDto reserveFunds(Long requestId, ReserveFundsDto dto, User adminUser) {
        verifyAdmin(adminUser);
        SupportRequest request = findSupportRequest(requestId);

        // Require completed two-person approval
        if (request.getStatus() != SupportRequestStatus.APPROVED && request.getStatus() != SupportRequestStatus.PAYMENT_PENDING) {
            throw new ApiException(
                    String.format("Funds can only be reserved for applications with completed two-person approval (Status: APPROVED). Current status: %s",
                            request.getStatus()),
                    HttpStatus.BAD_REQUEST
            );
        }

        // Prevent reserving twice for the same obligation
        if (reservationRepository.existsBySupportRequestIdAndReservationStatus(requestId, FundReservationStatus.RESERVED)) {
            throw new ApiException("An active fund reservation already exists for this support request", HttpStatus.CONFLICT);
        }

        BigDecimal reserveAmount = dto.getAmount() != null ? dto.getAmount() : request.getRequestedAmount();
        if (reserveAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Reservation amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        long reserveAmountPence = reserveAmount.multiply(BigDecimal.valueOf(100)).longValue();
        long totalLedgerBalancePence = fundLedgerRepository.sumTotalAvailableBalancePence();
        long activeReservationsPence = reservationRepository.sumActiveReservationsPence();
        long availableBalancePence = totalLedgerBalancePence - activeReservationsPence;

        if (reserveAmountPence > availableBalancePence) {
            // Cannot reserve - transition to WAITING_FOR_FUNDS
            request.setStatus(SupportRequestStatus.WAITING_FOR_FUNDS);
            supportRequestRepository.save(request);
            recordUpdate(request, SupportRequestStatus.APPROVED, SupportRequestStatus.WAITING_FOR_FUNDS,
                    "Insufficient community funds currently available. Queued in WAITING_FOR_FUNDS status.", adminUser);

            throw new ApiException(
                    String.format("Insufficient available communal fund balance. Available: £%.2f, Requested reservation: £%.2f. Application placed in WAITING_FOR_FUNDS status.",
                            availableBalancePence / 100.0, reserveAmount),
                    HttpStatus.BAD_REQUEST
            );
        }

        FundReservation reservation = FundReservation.builder()
                .supportRequest(request)
                .amount(reserveAmount)
                .amountPence(reserveAmountPence)
                .reservationStatus(FundReservationStatus.RESERVED)
                .reason(dto.getReason() != null ? dto.getReason().trim() : "Approved burial assistance")
                .reservedBy(adminUser)
                .build();
        FundReservation savedReservation = reservationRepository.save(reservation);

        request.setStatus(SupportRequestStatus.PAYMENT_PENDING);
        request.setUpdatedAt(LocalDateTime.now());
        supportRequestRepository.save(request);

        recordUpdate(request, SupportRequestStatus.APPROVED, SupportRequestStatus.PAYMENT_PENDING,
                String.format("Funds reserved (£%.2f). Burial provider payment pending.", reserveAmount), adminUser);
        logAudit(adminUser, "FUNDS_RESERVED", "FundReservation", savedReservation.getId(),
                "Reserved £" + reserveAmount + " for request #" + requestId);

        return mapReservationToDto(savedReservation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FundReservationResponseDto> listReservations(FundReservationStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FundReservation> reservations = (status != null)
                ? reservationRepository.findByReservationStatusOrderByCreatedAtDesc(status, pageable)
                : reservationRepository.findAllByOrderByCreatedAtDesc(pageable);
        return reservations.map(this::mapReservationToDto);
    }

    @Override
    @Transactional
    public FundReservationResponseDto releaseReservation(Long reservationId, ReleaseReservationDto dto, User adminUser) {
        verifyAdmin(adminUser);
        FundReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ApiException("Fund reservation not found with ID: " + reservationId, HttpStatus.NOT_FOUND));

        if (reservation.getReservationStatus() != FundReservationStatus.RESERVED) {
            throw new ApiException("Only active reservations (RESERVED) can be released", HttpStatus.BAD_REQUEST);
        }

        reservation.setReservationStatus(FundReservationStatus.RELEASED);
        reservation.setReleasedBy(adminUser);
        reservation.setReleasedAt(LocalDateTime.now());
        reservation.setReleaseReason(dto != null ? dto.getReason() : "Administrative release");
        FundReservation updated = reservationRepository.save(reservation);

        logAudit(adminUser, "FUND_RESERVATION_RELEASED", "FundReservation", reservationId,
                "Released reservation: " + (dto != null ? dto.getReason() : ""));

        return mapReservationToDto(updated);
    }

    // ==========================================
    // 6.4 Burial Provider Payments
    // ==========================================

    @Override
    @Transactional
    public ProviderPaymentResponseDto initiateProviderPayment(InitiateProviderPaymentDto dto, User adminUser) {
        verifyAdmin(adminUser);
        SupportRequest request = findSupportRequest(dto.getSupportRequestId());

        // 1. Require verified provider
        if (!Boolean.TRUE.equals(request.getProviderVerified())) {
            throw new ApiException("Burial provider details must be verified by an administrator prior to initiating payment", HttpStatus.BAD_REQUEST);
        }

        // 2. Require two-person approval
        long approvedCount = approvalRepository.countBySupportRequestIdAndDecision(request.getId(), ApprovalDecision.APPROVED);
        if (approvedCount < 2) {
            throw new ApiException("Payment requires completed two-person administrative approval", HttpStatus.BAD_REQUEST);
        }

        // 3. Require appropriate fund reservation
        FundReservation reservation = reservationRepository.findById(dto.getReservationId())
                .orElseThrow(() -> new ApiException("Fund reservation not found with ID: " + dto.getReservationId(), HttpStatus.NOT_FOUND));

        if (reservation.getReservationStatus() != FundReservationStatus.RESERVED) {
            throw new ApiException("Fund reservation must be in active RESERVED status", HttpStatus.BAD_REQUEST);
        }

        if (!reservation.getSupportRequest().getId().equals(request.getId())) {
            throw new ApiException("Reservation does not match the specified support request", HttpStatus.BAD_REQUEST);
        }

        // 4. Prevent duplicate provider payments
        if (providerPaymentRepository.existsByPaymentReference(dto.getPaymentReference().trim())) {
            throw new ApiException("Provider payment reference already exists: " + dto.getPaymentReference(), HttpStatus.CONFLICT);
        }

        long paymentAmountPence = dto.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        if (paymentAmountPence > reservation.getAmountPence()) {
            throw new ApiException(
                    String.format("Payment amount (£%.2f) exceeds reserved funds amount (£%.2f)",
                            dto.getAmount(), reservation.getAmount()),
                    HttpStatus.BAD_REQUEST
            );
        }

        ProviderPayment payment = ProviderPayment.builder()
                .supportRequest(request)
                .reservation(reservation)
                .providerName(request.getBurialProviderName())
                .amount(dto.getAmount())
                .amountPence(paymentAmountPence)
                .currency("GBP")
                .paymentReference(dto.getPaymentReference().trim())
                .status(ProviderPaymentStatus.PENDING)
                .initiatedBy(adminUser)
                .build();
        ProviderPayment savedPayment = providerPaymentRepository.save(payment);

        request.setStatus(SupportRequestStatus.PAYMENT_PENDING);
        request.setUpdatedAt(LocalDateTime.now());
        supportRequestRepository.save(request);

        recordUpdate(request, request.getStatus(), SupportRequestStatus.PAYMENT_PENDING,
                String.format("Provider disbursement of £%.2f initiated with reference %s.", dto.getAmount(), dto.getPaymentReference()), adminUser);
        logAudit(adminUser, "PROVIDER_PAYMENT_INITIATED", "ProviderPayment", savedPayment.getId(),
                "Reference: " + dto.getPaymentReference() + ", Amount: £" + dto.getAmount());

        return mapPaymentToDto(savedPayment);
    }

    @Override
    @Transactional
    public ProviderPaymentResponseDto confirmProviderPayment(Long paymentId, User adminUser) {
        verifyAdmin(adminUser);
        ProviderPayment payment = providerPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException("Provider payment not found with ID: " + paymentId, HttpStatus.NOT_FOUND));

        if (payment.getStatus() == ProviderPaymentStatus.PAID || payment.getStatus() == ProviderPaymentStatus.CONFIRMED) {
            throw new ApiException("Provider payment has already been confirmed as paid", HttpStatus.CONFLICT);
        }

        payment.setStatus(ProviderPaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setConfirmedBy(adminUser);
        ProviderPayment confirmedPayment = providerPaymentRepository.save(payment);

        // Update reservation to DISBURSED
        FundReservation reservation = payment.getReservation();
        reservation.setReservationStatus(FundReservationStatus.DISBURSED);
        reservationRepository.save(reservation);

        // Update support request to PAID
        SupportRequest request = payment.getSupportRequest();
        SupportRequestStatus prevStatus = request.getStatus();
        request.setStatus(SupportRequestStatus.PAID);
        request.setUpdatedAt(LocalDateTime.now());
        supportRequestRepository.save(request);

        // Post ledger disbursement entry: netAmount is negative
        long previousBalancePence = fundLedgerRepository.sumTotalAvailableBalancePence();
        long newRunningBalancePence = previousBalancePence - payment.getAmountPence();

        FundLedger ledgerEntry = FundLedger.builder()
                .entryType(FundLedgerEntryType.BURIAL_SUPPORT_DISBURSEMENT)
                .grossAmountPence(-payment.getAmountPence())
                .feePence(0L)
                .netAmountPence(-payment.getAmountPence())
                .currency("GBP")
                .runningBalancePence(newRunningBalancePence)
                .reference(payment.getPaymentReference())
                .description("Burial support assistance disbursed to provider: " + payment.getProviderName() + " for request #" + request.getId())
                .createdAt(LocalDateTime.now())
                .build();
        fundLedgerRepository.save(ledgerEntry);

        recordUpdate(request, prevStatus, SupportRequestStatus.PAID,
                String.format("Burial assistance of £%.2f successfully disbursed and settled to provider %s.",
                        payment.getAmount(), payment.getProviderName()), adminUser);
        logAudit(adminUser, "PROVIDER_PAYMENT_CONFIRMED", "ProviderPayment", paymentId,
                "Settled £" + payment.getAmount() + " to " + payment.getProviderName());

        return mapPaymentToDto(confirmedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProviderPaymentResponseDto> listProviderPayments(ProviderPaymentStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ProviderPayment> payments = (status != null)
                ? providerPaymentRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : providerPaymentRepository.findAllByOrderByCreatedAtDesc(pageable);
        return payments.map(this::mapPaymentToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderPaymentResponseDto getProviderPayment(Long paymentId) {
        ProviderPayment payment = providerPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException("Provider payment not found with ID: " + paymentId, HttpStatus.NOT_FOUND));
        return mapPaymentToDto(payment);
    }

    // ==========================================
    // 6.5 Refunds and Reversals
    // ==========================================

    @Override
    @Transactional
    public RefundResponseDto submitRefund(SubmitRefundDto dto, User adminUser) {
        verifyAdmin(adminUser);

        Donation donation = donationRepository.findById(dto.getOriginalTransactionId())
                .orElseThrow(() -> new ApiException("Original donation not found with ID: " + dto.getOriginalTransactionId(), HttpStatus.NOT_FOUND));

        if (donation.getPaymentStatus() != DonationPaymentStatus.SUCCEEDED &&
            donation.getPaymentStatus() != DonationPaymentStatus.PARTIALLY_REFUNDED) {
            throw new ApiException(
                    String.format("Only donations with SUCCEEDED status can be refunded. Current status: %s", donation.getPaymentStatus()),
                    HttpStatus.BAD_REQUEST
            );
        }

        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Refund amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        long refundAmountPence = dto.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        long alreadyRefundedPence = donation.getRefundedAmountPence() != null ? donation.getRefundedAmountPence() : 0L;
        long maxRefundablePence = donation.getAmountPence() - alreadyRefundedPence;

        if (refundAmountPence > maxRefundablePence) {
            throw new ApiException(
                    String.format("Refund amount (£%.2f) exceeds remaining refundable donation amount (£%.2f)",
                            dto.getAmount(), maxRefundablePence / 100.0),
                    HttpStatus.BAD_REQUEST
            );
        }

        // Update donation
        long updatedRefundedPence = alreadyRefundedPence + refundAmountPence;
        donation.setRefundedAmountPence(updatedRefundedPence);
        if (updatedRefundedPence >= donation.getAmountPence()) {
            donation.setPaymentStatus(DonationPaymentStatus.REFUNDED);
        } else {
            donation.setPaymentStatus(DonationPaymentStatus.PARTIALLY_REFUNDED);
        }
        donation.setUpdatedAt(LocalDateTime.now());
        donationRepository.save(donation);

        // Create FundRefund record
        FundRefund refund = FundRefund.builder()
                .originalTransactionId(dto.getOriginalTransactionId())
                .donation(donation)
                .amount(dto.getAmount())
                .amountPence(refundAmountPence)
                .currency("GBP")
                .reason(dto.getReason().trim())
                .status("COMPLETED")
                .processedBy(adminUser)
                .build();
        FundRefund savedRefund = refundRepository.save(refund);

        // Post ledger refund entry: gross and net are negative
        long previousBalancePence = fundLedgerRepository.sumTotalAvailableBalancePence();
        long newRunningBalancePence = previousBalancePence - refundAmountPence;

        FundLedger ledgerEntry = FundLedger.builder()
                .donationId(donation.getId())
                .entryType(FundLedgerEntryType.DONATION_REFUND)
                .grossAmountPence(-refundAmountPence)
                .feePence(0L)
                .netAmountPence(-refundAmountPence)
                .currency("GBP")
                .runningBalancePence(newRunningBalancePence)
                .reference("REFUND-" + savedRefund.getId())
                .description("Refund processed for donation #" + donation.getId() + ": " + dto.getReason())
                .createdAt(LocalDateTime.now())
                .build();
        fundLedgerRepository.save(ledgerEntry);

        logAudit(adminUser, "DONATION_REFUNDED", "FundRefund", savedRefund.getId(),
                "Refunded £" + dto.getAmount() + " for donation #" + donation.getId() + " - Reason: " + dto.getReason());

        return mapRefundToDto(savedRefund);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RefundResponseDto> listRefunds(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FundRefund> refunds = refundRepository.findAllByOrderByCreatedAtDesc(pageable);
        return refunds.map(this::mapRefundToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponseDto getRefund(Long refundId) {
        FundRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ApiException("Refund not found with ID: " + refundId, HttpStatus.NOT_FOUND));
        return mapRefundToDto(refund);
    }

    // ==========================================
    // 6.6 Fund Accounts, Ledger & Reports
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public Page<FundLedgerEntryDto> getFundLedger(FundLedgerEntryType entryType, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FundLedger> entries = fundLedgerRepository.findAllByOrderByCreatedAtDesc(pageable);

        return entries.map(e -> FundLedgerEntryDto.builder()
                .id(e.getId())
                .entryType(e.getEntryType())
                .donationId(e.getDonationId())
                .grossAmount(BigDecimal.valueOf(e.getGrossAmountPence()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .fee(BigDecimal.valueOf(e.getFeePence()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .netAmount(BigDecimal.valueOf(e.getNetAmountPence()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .runningBalance(BigDecimal.valueOf(e.getRunningBalancePence()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .currency(e.getCurrency())
                .reference(e.getReference())
                .description(e.getDescription())
                .createdAt(e.getCreatedAt())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public FundBalanceResponseDto getFundBalance() {
        long donationsGrossPence = fundLedgerRepository.sumSettledDonationGrossPence();
        long feesPence = fundLedgerRepository.sumTotalFeesPence();
        long refundsGrossPence = fundLedgerRepository.sumTotalRefundsGrossPence();
        long disbursementsPence = fundLedgerRepository.sumTotalDisbursementsPence();
        long activeReservationsPence = reservationRepository.sumActiveReservationsPence();
        long totalLedgerNetPence = fundLedgerRepository.sumTotalAvailableBalancePence();
        long availableBalancePence = totalLedgerNetPence - activeReservationsPence;

        return FundBalanceResponseDto.builder()
                .totalDonationsReceived(penceToBigDecimal(donationsGrossPence))
                .totalProcessingFees(penceToBigDecimal(feesPence))
                .totalRefunds(penceToBigDecimal(refundsGrossPence))
                .totalDisbursements(penceToBigDecimal(disbursementsPence))
                .activeReservations(penceToBigDecimal(activeReservationsPence))
                .availableFundBalance(penceToBigDecimal(availableBalancePence))
                .currency("GBP")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FundReportResponseDto getFundReport() {
        FundBalanceResponseDto balances = getFundBalance();
        long completedCases = supportRequestRepository.countByStatus(SupportRequestStatus.PAID);
        long totalDonors = fundLedgerRepository.countSettledDonationEntries();

        return FundReportResponseDto.builder()
                .donationTotals(balances.getTotalDonationsReceived())
                .refundTotals(balances.getTotalRefunds())
                .providerPaymentTotals(balances.getTotalDisbursements())
                .reservedFunds(balances.getActiveReservations())
                .availableBalance(balances.getAvailableFundBalance())
                .completedAssistanceCases(completedCases)
                .totalDonors(totalDonors)
                .currency("GBP")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportFundReportCsv() {
        FundReportResponseDto report = getFundReport();
        List<FundLedger> recentLedger = fundLedgerRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        StringBuilder csv = new StringBuilder();
        csv.append("COMMUNITY BURIAL FUND - FINANCIAL REPORT\n");
        csv.append("Generated At,").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");

        csv.append("METRIC,AMOUNT_GBP\n");
        csv.append("Total Donations Settled,").append(report.getDonationTotals()).append("\n");
        csv.append("Total Refunds Processed,").append(report.getRefundTotals()).append("\n");
        csv.append("Total Burial Provider Disbursements,").append(report.getProviderPaymentTotals()).append("\n");
        csv.append("Currently Reserved Funds,").append(report.getReservedFunds()).append("\n");
        csv.append("Available Unreserved Balance,").append(report.getAvailableBalance()).append("\n");
        csv.append("Completed Burial Assistance Cases,").append(report.getCompletedAssistanceCases()).append("\n");
        csv.append("Total Donation Settlements,").append(report.getTotalDonors()).append("\n\n");

        csv.append("TRANSACTION_ID,ENTRY_TYPE,GROSS_AMOUNT,FEE,NET_AMOUNT,RUNNING_BALANCE,REFERENCE,DATE\n");
        for (FundLedger entry : recentLedger) {
            csv.append(entry.getId()).append(",")
               .append(entry.getEntryType()).append(",")
               .append(penceToBigDecimal(entry.getGrossAmountPence())).append(",")
               .append(penceToBigDecimal(entry.getFeePence())).append(",")
               .append(penceToBigDecimal(entry.getNetAmountPence())).append(",")
               .append(penceToBigDecimal(entry.getRunningBalancePence())).append(",")
               .append("\"").append(entry.getReference().replace("\"", "\"\"")).append("\",")
               .append(entry.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        }

        return csv.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public FundImpactResponseDto getFundImpact() {
        long completedCases = supportRequestRepository.countByStatus(SupportRequestStatus.PAID);
        long disbursementsPence = fundLedgerRepository.sumTotalDisbursementsPence();
        BigDecimal totalDisbursed = penceToBigDecimal(disbursementsPence);

        BigDecimal average = BigDecimal.ZERO;
        if (completedCases > 0) {
            average = totalDisbursed.divide(BigDecimal.valueOf(completedCases), 2, RoundingMode.HALF_UP);
        }

        long activeApps = supportRequestRepository.count() - completedCases - supportRequestRepository.countByStatus(SupportRequestStatus.DECLINED);
        if (activeApps < 0) activeApps = 0;

        return FundImpactResponseDto.builder()
                .familiesAssisted(completedCases)
                .totalBurialSupportDisbursed(totalDisbursed)
                .averageAssistanceAmount(average)
                .activeApplications(activeApps)
                .currency("GBP")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getAuditLogs(String entityType, String action, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> logs = (entityType != null && !entityType.isBlank())
                ? auditLogRepository.findByEntityTypeOrderByCreatedAtDesc(entityType.trim(), pageable)
                : auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);

        return logs.map(a -> AuditLogResponseDto.builder()
                .id(a.getId())
                .actorUserId(a.getPerformedBy() != null ? a.getPerformedBy().getId() : null)
                .actorEmail(a.getPerformedBy() != null ? a.getPerformedBy().getEmail() : null)
                .action(a.getAction())
                .entityType(a.getEntityType())
                .entityId(a.getEntityId())
                .details(a.getDetails())
                .createdAt(a.getCreatedAt())
                .build());
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private SupportRequest findSupportRequest(Long id) {
        return supportRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException("Support request not found with ID: " + id, HttpStatus.NOT_FOUND));
    }

    private void verifyAdmin(User user) {
        if (!securityHelper.isAdminOrSuperAdmin(user)) {
            throw new ApiException("Access Denied: Only administrators can perform this action", HttpStatus.FORBIDDEN);
        }
    }

    private void recordUpdate(SupportRequest request, SupportRequestStatus prev, SupportRequestStatus next, String message, User user) {
        SupportRequestUpdate update = SupportRequestUpdate.builder()
                .supportRequest(request)
                .previousStatus(prev)
                .newStatus(next)
                .updateMessage(message)
                .updatedBy(user)
                .build();
        updateRepository.save(update);
    }

    private void logAudit(User actor, String action, String entityType, Long entityId, String details) {
        AuditLog audit = AuditLog.builder()
                .performedBy(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(audit);
    }

    private BigDecimal penceToBigDecimal(long pence) {
        return BigDecimal.valueOf(pence).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
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

    private SupportApprovalResponseDto mapApprovalToDto(SupportApproval a) {
        return SupportApprovalResponseDto.builder()
                .id(a.getId())
                .supportRequestId(a.getSupportRequest() != null ? a.getSupportRequest().getId() : null)
                .approverUserId(a.getApprover() != null ? a.getApprover().getId() : null)
                .approverName(a.getApprover() != null ? a.getApprover().getFullName() : null)
                .approverEmail(a.getApprover() != null ? a.getApprover().getEmail() : null)
                .decision(a.getDecision())
                .approvedAmount(a.getApprovedAmount())
                .decisionReason(a.getDecisionReason())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private FundReservationResponseDto mapReservationToDto(FundReservation r) {
        return FundReservationResponseDto.builder()
                .id(r.getId())
                .supportRequestId(r.getSupportRequest() != null ? r.getSupportRequest().getId() : null)
                .amount(r.getAmount())
                .reservationStatus(r.getReservationStatus())
                .reason(r.getReason())
                .reservedByUserId(r.getReservedBy() != null ? r.getReservedBy().getId() : null)
                .reservedByEmail(r.getReservedBy() != null ? r.getReservedBy().getEmail() : null)
                .releasedByUserId(r.getReleasedBy() != null ? r.getReleasedBy().getId() : null)
                .releaseReason(r.getReleaseReason())
                .createdAt(r.getCreatedAt())
                .releasedAt(r.getReleasedAt())
                .build();
    }

    private ProviderPaymentResponseDto mapPaymentToDto(ProviderPayment p) {
        return ProviderPaymentResponseDto.builder()
                .id(p.getId())
                .supportRequestId(p.getSupportRequest() != null ? p.getSupportRequest().getId() : null)
                .reservationId(p.getReservation() != null ? p.getReservation().getId() : null)
                .providerName(p.getProviderName())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .paymentReference(p.getPaymentReference())
                .status(p.getStatus())
                .initiatedByUserId(p.getInitiatedBy() != null ? p.getInitiatedBy().getId() : null)
                .confirmedByUserId(p.getConfirmedBy() != null ? p.getConfirmedBy().getId() : null)
                .createdAt(p.getCreatedAt())
                .paidAt(p.getPaidAt())
                .build();
    }

    private RefundResponseDto mapRefundToDto(FundRefund r) {
        return RefundResponseDto.builder()
                .id(r.getId())
                .originalTransactionId(r.getOriginalTransactionId())
                .donationId(r.getDonation() != null ? r.getDonation().getId() : null)
                .amount(r.getAmount())
                .currency(r.getCurrency())
                .reason(r.getReason())
                .status(r.getStatus())
                .processedByUserId(r.getProcessedBy() != null ? r.getProcessedBy().getId() : null)
                .createdAt(r.getCreatedAt())
                .completedAt(r.getCompletedAt())
                .build();
    }
}
