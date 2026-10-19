package com.rememberme.api.service.impl;

import com.rememberme.api.dto.response.DonationReceiptResponse;
import com.rememberme.api.entity.Donation;
import com.rememberme.api.entity.DonationReceipt;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.DonationReceiptRepository;
import com.rememberme.api.repository.DonationRepository;
import com.rememberme.api.service.DonationReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DonationReceiptServiceImpl implements DonationReceiptService {

    private final DonationReceiptRepository donationReceiptRepository;
    private final DonationRepository donationRepository;

    @Override
    @Transactional
    public synchronized DonationReceipt generateReceipt(Donation donation, String paymentMethod, String stripeChargeId, String receiptUrl) {
        // Enforce receipt uniqueness: one receipt per donation
        Optional<DonationReceipt> existing = donationReceiptRepository.findByDonationId(donation.getId());
        if (existing.isPresent()) {
            log.info("Receipt already exists for donationId: {}, receiptNumber: {}",
                    donation.getId(), existing.get().getReceiptNumber());
            return existing.get();
        }

        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        String receiptNumber = String.format("RCP-%s-%s", dateStr, suffix);

        DonationReceipt receipt = DonationReceipt.builder()
                .donation(donation)
                .receiptNumber(receiptNumber)
                .donorName(donation.getDonorName() != null ? donation.getDonorName() : "Valued Donor")
                .donorEmail(donation.getDonorEmail() != null ? donation.getDonorEmail() : "")
                .amountPence(donation.getAmountPence())
                .currency(donation.getCurrency() != null ? donation.getCurrency() : "GBP")
                .paymentMethod(paymentMethod != null ? paymentMethod : "card")
                .stripeChargeId(stripeChargeId != null ? stripeChargeId : donation.getStripeChargeId())
                .fundName("Community Burial Fund")
                .receiptUrl(receiptUrl)
                .issuedAt(LocalDateTime.now())
                .build();

        DonationReceipt saved = donationReceiptRepository.save(receipt);
        log.info("Issued official receipt {} for donationId: {}", receiptNumber, donation.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public DonationReceiptResponse getReceiptForDonor(Long donationId, Long requestingUserId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ApiException("Donation not found with id: " + donationId, HttpStatus.NOT_FOUND));

        if (!donation.getUser().getId().equals(requestingUserId)) {
            throw new ApiException("Access Denied: You can only view receipts for your own donations", HttpStatus.FORBIDDEN);
        }

        DonationReceipt receipt = donationReceiptRepository.findByDonationId(donationId)
                .orElseThrow(() -> new ApiException("Receipt not yet issued for donation id: " + donationId + ". Receipts are generated upon confirmed payment.", HttpStatus.NOT_FOUND));

        return DonationReceiptResponse.builder()
                .receiptNumber(receipt.getReceiptNumber())
                .donationId(donation.getId())
                .donorName(receipt.getDonorName())
                .donorEmail(receipt.getDonorEmail())
                .amountPence(receipt.getAmountPence())
                .amountFormatted(String.format("£%.2f", receipt.getAmountPence() / 100.0))
                .currency(receipt.getCurrency())
                .paymentMethod(receipt.getPaymentMethod())
                .fundName(receipt.getFundName())
                .receiptUrl(receipt.getReceiptUrl())
                .issuedAt(receipt.getIssuedAt())
                .build();
    }
}
