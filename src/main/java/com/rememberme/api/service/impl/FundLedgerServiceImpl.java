package com.rememberme.api.service.impl;

import com.rememberme.api.dto.response.FundSummaryResponse;
import com.rememberme.api.entity.Donation;
import com.rememberme.api.entity.FundLedger;
import com.rememberme.api.entity.FundLedgerEntryType;
import com.rememberme.api.repository.FundLedgerRepository;
import com.rememberme.api.service.FundLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FundLedgerServiceImpl implements FundLedgerService {

    private final FundLedgerRepository fundLedgerRepository;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized FundLedger recordDonationSettlement(Donation donation, Long feePence, String balanceTransactionId) {
        String reference = "SETTLE-DONATION-" + donation.getId();

        // Idempotency: Do not create duplicate settlement entries
        if (fundLedgerRepository.existsByReference(reference) ||
                fundLedgerRepository.existsByDonationIdAndEntryType(donation.getId(), FundLedgerEntryType.DONATION_SETTLEMENT)) {
            log.info("Settlement ledger entry already exists for donationId: {}", donation.getId());
            return fundLedgerRepository.findByReference(reference).orElse(null);
        }

        long grossPence = donation.getAmountPence();
        long actualFeePence = feePence != null ? feePence : 0L;
        long netPence = grossPence - actualFeePence;

        long currentRunningBalance = fundLedgerRepository.findTopByOrderByIdDesc()
                .map(FundLedger::getRunningBalancePence)
                .orElse(0L);

        long newRunningBalance = currentRunningBalance + netPence;

        FundLedger entry = FundLedger.builder()
                .donationId(donation.getId())
                .entryType(FundLedgerEntryType.DONATION_SETTLEMENT)
                .grossAmountPence(grossPence)
                .feePence(actualFeePence)
                .netAmountPence(netPence)
                .currency(donation.getCurrency() != null ? donation.getCurrency() : "GBP")
                .runningBalancePence(newRunningBalance)
                .stripeBalanceTransactionId(balanceTransactionId)
                .reference(reference)
                .description(String.format("Settlement of donation #%d: gross £%.2f, fee £%.2f, net £%.2f",
                        donation.getId(), grossPence / 100.0, actualFeePence / 100.0, netPence / 100.0))
                .createdAt(LocalDateTime.now())
                .build();

        FundLedger saved = fundLedgerRepository.save(entry);
        log.info("Recorded donation settlement in fund ledger. Entry ID: {}, Net: {} pence, New Running Balance: {} pence",
                saved.getId(), netPence, newRunningBalance);
        return saved;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized FundLedger recordDonationRefund(Donation donation, Long refundAmountPence, String refundId, String reason) {
        String reference = "REFUND-" + (refundId != null ? refundId : ("DONATION-" + donation.getId()));

        if (fundLedgerRepository.existsByReference(reference)) {
            log.info("Refund ledger entry already exists for reference: {}", reference);
            return fundLedgerRepository.findByReference(reference).orElse(null);
        }

        long refundPence = refundAmountPence != null ? refundAmountPence : donation.getAmountPence();
        long currentRunningBalance = fundLedgerRepository.findTopByOrderByIdDesc()
                .map(FundLedger::getRunningBalancePence)
                .orElse(0L);

        long netAdjustment = -refundPence;
        long newRunningBalance = currentRunningBalance + netAdjustment;

        FundLedger entry = FundLedger.builder()
                .donationId(donation.getId())
                .entryType(FundLedgerEntryType.DONATION_REFUND)
                .grossAmountPence(-refundPence)
                .feePence(0L)
                .netAmountPence(netAdjustment)
                .currency(donation.getCurrency() != null ? donation.getCurrency() : "GBP")
                .runningBalancePence(newRunningBalance)
                .reference(reference)
                .description(String.format("Refund for donation #%d: -£%.2f%s",
                        donation.getId(), refundPence / 100.0, (reason != null ? " (" + reason + ")" : "")))
                .createdAt(LocalDateTime.now())
                .build();

        FundLedger saved = fundLedgerRepository.save(entry);
        log.info("Recorded refund in fund ledger. Entry ID: {}, Net Adjustment: {} pence, New Running Balance: {} pence",
                saved.getId(), netAdjustment, newRunningBalance);
        return saved;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized FundLedger recordDisputeAdjustment(Donation donation, Long disputedAmountPence, Long disputeFeePence, String disputeId) {
        String reference = "DISPUTE-" + (disputeId != null ? disputeId : ("DONATION-" + donation.getId()));

        if (fundLedgerRepository.existsByReference(reference)) {
            log.info("Dispute ledger entry already exists for reference: {}", reference);
            return fundLedgerRepository.findByReference(reference).orElse(null);
        }

        long disputeAmount = disputedAmountPence != null ? disputedAmountPence : donation.getAmountPence();
        long fee = disputeFeePence != null ? disputeFeePence : 0L;
        long netAdjustment = -(disputeAmount + fee);

        long currentRunningBalance = fundLedgerRepository.findTopByOrderByIdDesc()
                .map(FundLedger::getRunningBalancePence)
                .orElse(0L);

        long newRunningBalance = currentRunningBalance + netAdjustment;

        FundLedger entry = FundLedger.builder()
                .donationId(donation.getId())
                .entryType(FundLedgerEntryType.DISPUTE_ADJUSTMENT)
                .grossAmountPence(-disputeAmount)
                .feePence(fee)
                .netAmountPence(netAdjustment)
                .currency(donation.getCurrency() != null ? donation.getCurrency() : "GBP")
                .runningBalancePence(newRunningBalance)
                .reference(reference)
                .description(String.format("Dispute deduction for donation #%d: -£%.2f, dispute fee £%.2f",
                        donation.getId(), disputeAmount / 100.0, fee / 100.0))
                .createdAt(LocalDateTime.now())
                .build();

        FundLedger saved = fundLedgerRepository.save(entry);
        log.info("Recorded dispute adjustment in fund ledger. Entry ID: {}, Net Adjustment: {} pence, New Running Balance: {} pence",
                saved.getId(), netAdjustment, newRunningBalance);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public FundSummaryResponse getFundSummary() {
        long settledCount = fundLedgerRepository.countSettledDonationEntries();
        long grossPence = fundLedgerRepository.sumSettledDonationGrossPence();
        long feesPence = fundLedgerRepository.sumTotalFeesPence();
        long refundsPence = fundLedgerRepository.sumTotalRefundsGrossPence();
        long availableBalancePence = fundLedgerRepository.sumTotalAvailableBalancePence();

        return FundSummaryResponse.builder()
                .currency("GBP")
                .currencySymbol("£")
                .totalSettledDonationsCount(settledCount)
                .totalGrossPence(grossPence)
                .totalFeesPence(feesPence)
                .totalRefundsPence(refundsPence)
                .availableBalancePence(availableBalancePence)
                .availableBalanceFormatted(String.format("£%,.2f", availableBalancePence / 100.0))
                .fundPurpose("The Remember Me Community Burial Fund provides dignified, communal burial assistance to families facing hardship. Every donation directly increases the available fund pool.")
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public long getAvailableBalancePence() {
        return fundLedgerRepository.sumTotalAvailableBalancePence();
    }
}
