package com.rememberme.api.communityfund.service.impl;

import com.rememberme.api.communityfund.config.CommunityFundProperties;
import com.rememberme.api.communityfund.dto.request.DonationCheckoutRequest;
import com.rememberme.api.communityfund.dto.response.*;
import com.rememberme.api.communityfund.entity.PaymentTransaction;
import com.rememberme.api.communityfund.exception.CommunityFundAccessDeniedException;
import com.rememberme.api.communityfund.exception.CommunityFundNotFoundException;
import com.rememberme.api.communityfund.exception.CommunityFundValidationException;
import com.rememberme.api.communityfund.mapper.FundMapper;
import com.rememberme.api.communityfund.repository.PaymentTransactionRepository;
import com.rememberme.api.communityfund.security.CommunityFundSecurityHelper;
import com.rememberme.api.communityfund.service.CommunityDonationService;
import com.rememberme.api.entity.*;
import com.rememberme.api.exception.InvalidPaginationException;
import com.rememberme.api.repository.DonationReceiptRepository;
import com.rememberme.api.repository.DonationRepository;
import com.rememberme.api.service.StripePaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityDonationServiceImpl implements CommunityDonationService {

    private final DonationRepository donationRepository;
    private final DonationReceiptRepository donationReceiptRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final StripePaymentService stripePaymentService;
    private final CommunityFundProperties properties;
    private final FundMapper fundMapper;
    private final CommunityFundSecurityHelper securityHelper;

    @Override
    @Transactional(readOnly = true)
    public DonationAmountsResponse getDonationAmounts() {
        List<DonationAmountsResponse.PresetAmountOption> presetOptions = List.of(
                DonationAmountsResponse.PresetAmountOption.builder()
                        .amount(BigDecimal.valueOf(5.00))
                        .formatted("£5.00")
                        .description("Contributes to cemetery upkeep and communal dignity")
                        .build(),
                DonationAmountsResponse.PresetAmountOption.builder()
                        .amount(BigDecimal.valueOf(10.00))
                        .formatted("£10.00")
                        .description("Helps support essential funeral and burial service fees")
                        .build(),
                DonationAmountsResponse.PresetAmountOption.builder()
                        .amount(BigDecimal.valueOf(50.00))
                        .formatted("£50.00")
                        .description("Substantially relieves funeral poverty for families facing severe hardship")
                        .build()
        );

        DonationAmountsResponse.CustomAmountConfig customConfig = DonationAmountsResponse.CustomAmountConfig.builder()
                .enabled(true)
                .minAmount(properties.getMinDonationAmount())
                .maxAmount(properties.getMaxDonationAmount())
                .minAmountFormatted(fundMapper.formatCurrency(properties.getMinDonationAmount()))
                .maxAmountFormatted(fundMapper.formatCurrency(properties.getMaxDonationAmount()))
                .build();

        return DonationAmountsResponse.builder()
                .currency(properties.getCurrency())
                .currencySymbol(properties.getCurrencySymbol())
                .presets(presetOptions)
                .customAmount(customConfig)
                .explanation("All donations flow directly into the shared Community Burial Fund pool. Loved one dedications are commemorative tributes and prayers, and never ring-fence money to specific plots or individuals.")
                .build();
    }

    @Override
    @Transactional
    public DonationCheckoutResponse createCheckout(User donor, DonationCheckoutRequest request, String idempotencyKey) {
        validateCheckoutRequest(request);

        // 1. Check idempotency if client provided a key
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<Donation> existingDonation = donationRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existingDonation.isPresent()) {
                Donation existing = existingDonation.get();
                if (existing.getUser().getId().equals(donor.getId())) {
                    log.info("Idempotent checkout hit for key '{}', returning existing donation #{}", idempotencyKey, existing.getId());
                    return buildCheckoutResponse(existing);
                }
            }
        }

        BigDecimal amount = request.getAmount();
        long amountPence = amount.multiply(BigDecimal.valueOf(100)).longValue();

        // 2. Build and persist pending Donation record
        Donation donation = Donation.builder()
                .user(donor)
                .amountPence(amountPence)
                .currency(properties.getCurrency())
                .paymentStatus(DonationPaymentStatus.PENDING)
                .settlementStatus(DonationSettlementStatus.PENDING)
                .paymentProvider("STRIPE")
                .feePence(0L)
                .netAmountPence(0L)
                .refundedAmountPence(0L)
                .donorName(donor.getFullName())
                .donorEmail(donor.getEmail())
                .idempotencyKey(idempotencyKey != null && !idempotencyKey.trim().isEmpty() ? idempotencyKey.trim() : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 3. Attach optional dedications
        List<String> dedicationNames = new ArrayList<>();
        String dedicationMessage = null;
        if (request.getDedication() != null && request.getDedication().isEnabled()) {
            dedicationNames = request.getDedication().resolveLovedOneNames();
            dedicationMessage = request.getDedication().getMessage();
            for (String name : dedicationNames) {
                DonationDedication dedication = DonationDedication.builder()
                        .donation(donation)
                        .dedicationName(name)
                        .lovedOneName(name)
                        .message(dedicationMessage)
                        .dedicationMessage(dedicationMessage)
                        .createdAt(LocalDateTime.now())
                        .build();
                donation.getDedications().add(dedication);
            }
        }

        Donation savedDonation = donationRepository.save(donation);

        // 4. Initiate Payment with Payment Provider (Stripe)
        PaymentIntent paymentIntent;
        try {
            paymentIntent = stripePaymentService.createPaymentIntent(
                    amountPence,
                    properties.getCurrency(),
                    savedDonation.getId(),
                    donor.getEmail(),
                    idempotencyKey
            );
        } catch (StripeException e) {
            log.error("Stripe PaymentIntent creation failed for donation #{}: {}", savedDonation.getId(), e.getMessage());
            savedDonation.setPaymentStatus(DonationPaymentStatus.FAILED);
            donationRepository.save(savedDonation);
            throw new CommunityFundValidationException("Payment checkout initiation failed: " + e.getMessage());
        }

        savedDonation.setStripePaymentIntentId(paymentIntent.getId());
        savedDonation.setProviderPaymentId(paymentIntent.getId());
        savedDonation.setClientSecret(paymentIntent.getClientSecret());
        savedDonation.setStripeCustomerId(paymentIntent.getCustomer());
        donationRepository.save(savedDonation);

        // 5. Record initial audit transaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .donation(savedDonation)
                .providerTransactionId(paymentIntent.getId())
                .amount(amount)
                .amountPence(amountPence)
                .currency(properties.getCurrency())
                .status("PENDING")
                .transactionType("CHECKOUT_INITIATED")
                .createdAt(LocalDateTime.now())
                .build();
        paymentTransactionRepository.save(transaction);

        log.info("Created donation checkout #{} with Stripe PI: {}", savedDonation.getId(), paymentIntent.getId());
        return buildCheckoutResponse(savedDonation);
    }

    @Override
    @Transactional(readOnly = true)
    public DonationDetailsResponse getDonationDetails(Long donationId, User user) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new CommunityFundNotFoundException("Donation", donationId));

        securityHelper.verifyOwnerOrStaff(donation.getUser().getId());

        return mapToDonationDetailsResponse(donation);
    }

    @Override
    @Transactional(readOnly = true)
    public DonationReceiptInfoResponse getDonationReceipt(Long donationId, User user) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new CommunityFundNotFoundException("Donation", donationId));

        securityHelper.verifyOwnerOrStaff(donation.getUser().getId());

        if (donation.getPaymentStatus() != DonationPaymentStatus.SUCCEEDED) {
            throw new CommunityFundValidationException("Official receipt is only available for confirmed, successful donations. Current status: " + donation.getPaymentStatus());
        }

        DonationReceipt receipt = donationReceiptRepository.findByDonationId(donationId)
                .orElseThrow(() -> new CommunityFundNotFoundException("Official receipt for donation #" + donationId + " is not yet available."));

        return fundMapper.toDonationReceiptInfoResponse(receipt);
    }

    @Override
    @Transactional
    public DonationRetryResponse retryDonation(Long donationId, User user) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new CommunityFundNotFoundException("Donation", donationId));

        securityHelper.verifyOwnerOrStaff(donation.getUser().getId());

        if (donation.getPaymentStatus() == DonationPaymentStatus.SUCCEEDED) {
            throw new CommunityFundValidationException("Donation #" + donationId + " has already been successfully confirmed. Retrying is prohibited to prevent duplicate financial charges.");
        }

        String freshClientSecret = null;
        String piId = donation.getStripePaymentIntentId();

        try {
            if (piId != null && !piId.isBlank()) {
                PaymentIntent existingPi = stripePaymentService.retrievePaymentIntent(piId);
                if (existingPi != null && !"canceled".equalsIgnoreCase(existingPi.getStatus())) {
                    freshClientSecret = existingPi.getClientSecret();
                }
            }

            if (freshClientSecret == null || freshClientSecret.isBlank()) {
                PaymentIntent newPi = stripePaymentService.createPaymentIntent(
                        donation.getAmountPence(),
                        donation.getCurrency(),
                        donation.getId(),
                        donation.getDonorEmail(),
                        null
                );
                piId = newPi.getId();
                freshClientSecret = newPi.getClientSecret();
                donation.setStripePaymentIntentId(piId);
                donation.setProviderPaymentId(piId);
            }
        } catch (StripeException e) {
            log.error("Failed to refresh Stripe PaymentIntent for donation #{}: {}", donationId, e.getMessage());
            throw new CommunityFundValidationException("Failed to retry donation payment: " + e.getMessage());
        }

        donation.setClientSecret(freshClientSecret);
        donation.setPaymentStatus(DonationPaymentStatus.PENDING);
        donation.setUpdatedAt(LocalDateTime.now());
        donationRepository.save(donation);

        PaymentTransaction retryTx = PaymentTransaction.builder()
                .donation(donation)
                .providerTransactionId(piId)
                .amount(fundMapper.penceToBigDecimal(donation.getAmountPence()))
                .amountPence(donation.getAmountPence())
                .currency(donation.getCurrency())
                .status("PENDING")
                .transactionType("RETRY_INITIATED")
                .createdAt(LocalDateTime.now())
                .build();
        paymentTransactionRepository.save(retryTx);

        log.info("Safely initiated retry for donation #{}, refreshed PI: {}", donationId, piId);

        return DonationRetryResponse.builder()
                .donationId(donation.getId())
                .clientSecret(freshClientSecret)
                .stripePaymentIntentId(piId)
                .amount(fundMapper.penceToBigDecimal(donation.getAmountPence()))
                .currency(donation.getCurrency())
                .status("PENDING")
                .message("Payment session refreshed successfully. Complete checkout using the provided client secret.")
                .build();
    }

    private void validateCheckoutRequest(DonationCheckoutRequest request) {
        if (request == null) {
            throw new CommunityFundValidationException("Donation request cannot be null");
        }
        if (request.getAmount() == null) {
            throw new CommunityFundValidationException("Donation amount is required");
        }
        if (request.getAmount().compareTo(properties.getMinDonationAmount()) < 0) {
            throw new CommunityFundValidationException(String.format("Donation amount must be at least %s", fundMapper.formatCurrency(properties.getMinDonationAmount())));
        }
        if (request.getAmount().compareTo(properties.getMaxDonationAmount()) > 0) {
            throw new CommunityFundValidationException(String.format("Donation amount cannot exceed %s", fundMapper.formatCurrency(properties.getMaxDonationAmount())));
        }
        if (request.getCurrency() != null && !request.getCurrency().trim().equalsIgnoreCase("GBP")) {
            throw new CommunityFundValidationException("Only GBP currency is currently supported");
        }
    }

    private DonationCheckoutResponse buildCheckoutResponse(Donation donation) {
        BigDecimal amount = fundMapper.penceToBigDecimal(donation.getAmountPence());

        List<String> dedicationNames = new ArrayList<>();
        String dedicationMessage = null;
        if (donation.getDedications() != null) {
            for (DonationDedication d : donation.getDedications()) {
                if (d.getLovedOneName() != null) {
                    dedicationNames.add(d.getLovedOneName());
                } else if (d.getDedicationName() != null) {
                    dedicationNames.add(d.getDedicationName());
                }
                if (dedicationMessage == null && d.getMessage() != null) {
                    dedicationMessage = d.getMessage();
                }
            }
        }

        return DonationCheckoutResponse.builder()
                .donationId(donation.getId())
                .clientSecret(donation.getClientSecret())
                .stripePaymentIntentId(donation.getStripePaymentIntentId())
                .paymentProvider(donation.getPaymentProvider() != null ? donation.getPaymentProvider() : "STRIPE")
                .amount(amount)
                .amountFormatted(fundMapper.formatCurrency(amount))
                .currency(donation.getCurrency())
                .currencySymbol("£")
                .paymentStatus(donation.getPaymentStatus() != null ? donation.getPaymentStatus().name() : "PENDING")
                .dedicationNames(dedicationNames)
                .dedicationMessage(dedicationMessage)
                .checkoutInstructions("Pass clientSecret to Stripe Mobile PaymentSheet or Web Elements.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DonationDetailsResponse> getMyDonations(User user, int page, int size, String sortBy, String direction) {
        if (user == null || user.getId() == null) {
            throw new CommunityFundAccessDeniedException("Authenticated user context is required to view donation history");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidPaginationException("Page index must be non-negative and size must be between 1 and 100");
        }

        String sortProperty = resolveSortProperty(sortBy);
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));

        Page<Donation> donationPage = donationRepository.findByUserId(user.getId(), pageable);
        return donationPage.map(this::mapToDonationDetailsResponse);
    }

    private DonationDetailsResponse mapToDonationDetailsResponse(Donation donation) {
        List<String> names = new ArrayList<>();
        String message = null;
        if (donation.getDedications() != null) {
            for (DonationDedication d : donation.getDedications()) {
                if (d.getLovedOneName() != null) {
                    names.add(d.getLovedOneName());
                } else if (d.getDedicationName() != null) {
                    names.add(d.getDedicationName());
                }
                if (message == null && d.getMessage() != null) {
                    message = d.getMessage();
                }
            }
        }

        Optional<DonationReceipt> receiptOpt = donationReceiptRepository.findByDonationId(donation.getId());
        boolean receiptAvailable = receiptOpt.isPresent();
        String receiptNumber = receiptOpt.map(DonationReceipt::getReceiptNumber).orElse(null);

        return fundMapper.toDonationDetailsResponse(donation, names, message, receiptAvailable, receiptNumber);
    }

    private String resolveSortProperty(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt";
        }
        return switch (sortBy.trim().toLowerCase()) {
            case "amount", "amountpence" -> "amountPence";
            case "paymentstatus", "status" -> "paymentStatus";
            case "id" -> "id";
            case "createdat", "date" -> "createdAt";
            default -> "createdAt";
        };
    }
}
