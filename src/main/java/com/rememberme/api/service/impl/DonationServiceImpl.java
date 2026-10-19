package com.rememberme.api.service.impl;

import com.rememberme.api.config.StripeConfig;
import com.rememberme.api.dto.request.CreateDonationPaymentIntentRequest;
import com.rememberme.api.dto.request.DedicationRequest;
import com.rememberme.api.dto.response.*;
import com.rememberme.api.entity.Donation;
import com.rememberme.api.entity.DonationDedication;
import com.rememberme.api.entity.DonationPaymentStatus;
import com.rememberme.api.entity.DonationSettlementStatus;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.DeceasedPersonRepository;
import com.rememberme.api.repository.DonationDedicationRepository;
import com.rememberme.api.repository.DonationReceiptRepository;
import com.rememberme.api.repository.DonationRepository;
import com.rememberme.api.repository.MemorialRepository;
import com.rememberme.api.service.DonationService;
import com.rememberme.api.service.StripePaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DonationServiceImpl implements DonationService {

    private final DonationRepository donationRepository;
    private final DonationDedicationRepository donationDedicationRepository;
    private final DonationReceiptRepository donationReceiptRepository;
    private final DeceasedPersonRepository deceasedPersonRepository;
    private final MemorialRepository memorialRepository;
    private final StripePaymentService stripePaymentService;
    private final StripeConfig stripeConfig;

    private static final long MIN_AMOUNT_PENCE = 100L;       // £1.00
    private static final long MAX_AMOUNT_PENCE = 500000L;    // £5,000.00

    @Override
    public DonationOptionsResponse getDonationOptions() {
        List<PresetAmountDto> presets = List.of(
                PresetAmountDto.builder().amountPence(500L).displayLabel("£5").build(),
                PresetAmountDto.builder().amountPence(1000L).displayLabel("£10").build(),
                PresetAmountDto.builder().amountPence(5000L).displayLabel("£50").build()
        );

        return DonationOptionsResponse.builder()
                .currency("GBP")
                .currencySymbol("£")
                .presetAmounts(presets)
                .minCustomAmountPence(MIN_AMOUNT_PENCE)
                .maxCustomAmountPence(MAX_AMOUNT_PENCE)
                .sharedFundExplanation("Every contribution directly strengthens our single Community Burial Fund, helping ensure that every community member in need receives a respectful burial. Loved one dedications serve as meaningful commemorative tributes but never restrict or allocate funds to individuals or graves.")
                .stripePublishableKey(stripeConfig.getPublishableKey())
                .build();
    }

    @Override
    @Transactional
    public CreatePaymentIntentResponse createPaymentIntent(User donor, CreateDonationPaymentIntentRequest request, String headerIdempotencyKey) {
        if (donor == null || donor.getId() == null) {
            throw new ApiException("Authentication required to make a donation", HttpStatus.UNAUTHORIZED);
        }

        // 1. Validate amount on the server
        Long amountPence = request.getAmountPence();
        if (amountPence == null || amountPence < MIN_AMOUNT_PENCE || amountPence > MAX_AMOUNT_PENCE) {
            throw new ApiException(
                    String.format("Invalid donation amount: £%.2f. Must be between £%.2f and £%.2f",
                            (amountPence != null ? amountPence / 100.0 : 0.0),
                            MIN_AMOUNT_PENCE / 100.0,
                            MAX_AMOUNT_PENCE / 100.0),
                    HttpStatus.BAD_REQUEST);
        }

        // 2. Validate currency
        String currency = request.getCurrency() != null && !request.getCurrency().isBlank()
                ? request.getCurrency().toUpperCase()
                : "GBP";
        if (!"GBP".equals(currency)) {
            throw new ApiException("Only GBP currency is accepted for the Community Burial Fund", HttpStatus.BAD_REQUEST);
        }

        // 3. Resolve idempotency key
        String idempotencyKey = headerIdempotencyKey != null && !headerIdempotencyKey.isBlank()
                ? headerIdempotencyKey
                : request.getIdempotencyKey();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Donation> existing = donationRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Donation existingDonation = existing.get();
                if (!existingDonation.getUser().getId().equals(donor.getId())) {
                    throw new ApiException("Idempotency key collision detected", HttpStatus.CONFLICT);
                }
                log.info("Reusing existing donation {} for idempotency key {}", existingDonation.getId(), idempotencyKey);
                return CreatePaymentIntentResponse.builder()
                        .donationId(existingDonation.getId())
                        .clientSecret(existingDonation.getClientSecret())
                        .paymentIntentId(existingDonation.getStripePaymentIntentId())
                        .amountPence(existingDonation.getAmountPence())
                        .currency(existingDonation.getCurrency())
                        .status(existingDonation.getPaymentStatus().name())
                        .idempotencyKey(idempotencyKey)
                        .build();
            }
        }

        // 4. Validate dedication targets if IDs provided
        if (request.getDedications() != null) {
            for (DedicationRequest d : request.getDedications()) {
                if (d.getDeceasedPersonId() != null && !deceasedPersonRepository.existsById(d.getDeceasedPersonId())) {
                    throw new ApiException("Referenced deceased person ID " + d.getDeceasedPersonId() + " does not exist", HttpStatus.BAD_REQUEST);
                }
                if (d.getMemorialId() != null && !memorialRepository.existsById(d.getMemorialId())) {
                    throw new ApiException("Referenced memorial ID " + d.getMemorialId() + " does not exist", HttpStatus.BAD_REQUEST);
                }
            }
        }

        // 5. Create pending donation record
        Donation donation = Donation.builder()
                .user(donor)
                .amountPence(amountPence)
                .currency(currency)
                .paymentStatus(DonationPaymentStatus.PENDING)
                .settlementStatus(DonationSettlementStatus.PENDING)
                .feePence(0L)
                .netAmountPence(0L)
                .refundedAmountPence(0L)
                .idempotencyKey(idempotencyKey)
                .donorName(donor.getFullName())
                .donorEmail(donor.getEmail())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Donation savedDonation = donationRepository.save(donation);

        // 6. Save dedications (separate commemorative links without money allocation)
        if (request.getDedications() != null && !request.getDedications().isEmpty()) {
            List<DonationDedication> dedications = new ArrayList<>();
            for (DedicationRequest dr : request.getDedications()) {
                DonationDedication dd = DonationDedication.builder()
                        .donation(savedDonation)
                        .dedicationName(dr.getDedicationName())
                        .deceasedPersonId(dr.getDeceasedPersonId())
                        .memorialId(dr.getMemorialId())
                        .message(dr.getMessage())
                        .createdAt(LocalDateTime.now())
                        .build();
                dedications.add(dd);
            }
            donationDedicationRepository.saveAll(dedications);
            savedDonation.setDedications(dedications);
        }

        // 7. Invoke Stripe API securely with internal donation ID in metadata
        try {
            PaymentIntent paymentIntent = stripePaymentService.createPaymentIntent(
                    amountPence,
                    currency,
                    savedDonation.getId(),
                    donor.getEmail(),
                    idempotencyKey
            );

            savedDonation.setStripePaymentIntentId(paymentIntent.getId());
            savedDonation.setClientSecret(paymentIntent.getClientSecret());
            donationRepository.save(savedDonation);

            log.info("Created Stripe PaymentIntent {} for donation {}", paymentIntent.getId(), savedDonation.getId());

            return CreatePaymentIntentResponse.builder()
                    .donationId(savedDonation.getId())
                    .clientSecret(paymentIntent.getClientSecret())
                    .paymentIntentId(paymentIntent.getId())
                    .amountPence(amountPence)
                    .currency(currency)
                    .status(savedDonation.getPaymentStatus().name())
                    .idempotencyKey(idempotencyKey)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to create Stripe PaymentIntent for donation {}: {}", savedDonation.getId(), e.getMessage(), e);
            savedDonation.setPaymentStatus(DonationPaymentStatus.FAILED);
            donationRepository.save(savedDonation);
            throw new ApiException("Payment provider error: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DonationResponse> getMyDonations(User donor, Pageable pageable) {
        Page<Donation> donations = donationRepository.findByUser(donor, pageable);
        return donations.map(this::mapToDonationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DonationResponse getMyDonationById(Long donationId, Long requestingUserId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ApiException("Donation not found with id: " + donationId, HttpStatus.NOT_FOUND));

        if (!donation.getUser().getId().equals(requestingUserId)) {
            throw new ApiException("Access Denied: You can only view your own donations", HttpStatus.FORBIDDEN);
        }

        return mapToDonationResponse(donation);
    }

    private DonationResponse mapToDonationResponse(Donation donation) {
        List<DedicationResponse> dedications = donationDedicationRepository.findByDonationId(donation.getId())
                .stream()
                .map(d -> DedicationResponse.builder()
                        .id(d.getId())
                        .dedicationName(d.getDedicationName())
                        .deceasedPersonId(d.getDeceasedPersonId())
                        .memorialId(d.getMemorialId())
                        .message(d.getMessage())
                        .build())
                .collect(Collectors.toList());

        Optional<String> receiptNumber = donationReceiptRepository.findByDonationId(donation.getId())
                .map(r -> r.getReceiptNumber());

        return DonationResponse.builder()
                .id(donation.getId())
                .amountPence(donation.getAmountPence())
                .amountFormatted(String.format("£%.2f", donation.getAmountPence() / 100.0))
                .currency(donation.getCurrency())
                .paymentStatus(donation.getPaymentStatus().name())
                .settlementStatus(donation.getSettlementStatus().name())
                .donorName(donation.getDonorName())
                .donorEmail(donation.getDonorEmail())
                .dedications(dedications)
                .hasReceipt(receiptNumber.isPresent())
                .receiptNumber(receiptNumber.orElse(null))
                .createdAt(donation.getCreatedAt())
                .succeededAt(donation.getSucceededAt())
                .settledAt(donation.getSettledAt())
                .build();
    }
}
