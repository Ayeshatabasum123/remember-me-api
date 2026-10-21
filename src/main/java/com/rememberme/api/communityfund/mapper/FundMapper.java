package com.rememberme.api.communityfund.mapper;

import com.rememberme.api.communityfund.dto.response.FundHomeResponse;
import com.rememberme.api.communityfund.dto.response.FundImpactResponse;
import com.rememberme.api.communityfund.dto.response.FundPublicBalanceSummaryResponse;
import com.rememberme.api.communityfund.entity.FundSettings;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;

@Component
public class FundMapper {

    private static final NumberFormat GBP_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);

    public FundHomeResponse toFundHomeResponse(FundSettings settings) {
        if (settings == null) {
            return null;
        }
        return FundHomeResponse.builder()
                .fundName(settings.getFundName())
                .introduction(settings.getIntroduction())
                .purpose(settings.getPurpose())
                .description(settings.getDescription())
                .donationInformation(settings.getDonationInformation())
                .privacyInformation(settings.getPrivacyInformation())
                .fundingDisclaimer(settings.getFundingDisclaimer())
                .updatedAt(settings.getUpdatedAt() != null ? settings.getUpdatedAt() : settings.getCreatedAt())
                .build();
    }

    public FundImpactResponse toFundImpactResponse(
            long confirmedDonationsCount,
            long grossPenceReceived,
            long completedCasesCount,
            long disbursedPence,
            String currency,
            String currencySymbol) {

        BigDecimal totalReceived = penceToBigDecimal(grossPenceReceived);
        BigDecimal totalDisbursed = penceToBigDecimal(disbursedPence);

        return FundImpactResponse.builder()
                .currency(currency)
                .currencySymbol(currencySymbol)
                .totalConfirmedDonations(confirmedDonationsCount)
                .totalAmountReceived(totalReceived)
                .totalAmountReceivedFormatted(formatCurrency(totalReceived))
                .totalCompletedSupportCases(completedCasesCount)
                .totalApprovedSupportDisbursed(totalDisbursed)
                .totalApprovedSupportDisbursedFormatted(formatCurrency(totalDisbursed))
                .asOf(LocalDateTime.now())
                .build();
    }

    public FundPublicBalanceSummaryResponse toPublicBalanceSummaryResponse(
            long confirmedDonationsCount,
            long grossPenceReceived,
            long disbursedPence,
            long availableBalancePence,
            String currency,
            String currencySymbol) {

        BigDecimal totalReceived = penceToBigDecimal(grossPenceReceived);
        BigDecimal totalDisbursed = penceToBigDecimal(disbursedPence);
        BigDecimal availableBalance = penceToBigDecimal(availableBalancePence);

        return FundPublicBalanceSummaryResponse.builder()
                .currency(currency)
                .currencySymbol(currencySymbol)
                .totalConfirmedDonations(confirmedDonationsCount)
                .totalAmountReceived(totalReceived)
                .totalAmountReceivedFormatted(formatCurrency(totalReceived))
                .totalApprovedSupportDisbursed(totalDisbursed)
                .totalApprovedSupportDisbursedFormatted(formatCurrency(totalDisbursed))
                .availableBalance(availableBalance)
                .availableBalanceFormatted(formatCurrency(availableBalance))
                .asOf(LocalDateTime.now())
                .build();
    }

    public BigDecimal penceToBigDecimal(long pence) {
        return BigDecimal.valueOf(pence)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "£0.00";
        }
        return GBP_FORMAT.format(amount);
    }

    public com.rememberme.api.communityfund.dto.response.DonationDetailsResponse toDonationDetailsResponse(
            com.rememberme.api.entity.Donation donation,
            java.util.List<String> lovedOneNames,
            String dedicationMessage,
            boolean receiptAvailable,
            String receiptNumber) {

        BigDecimal amount = penceToBigDecimal(donation.getAmountPence());

        return com.rememberme.api.communityfund.dto.response.DonationDetailsResponse.builder()
                .id(donation.getId())
                .donorName(donation.getDonorName())
                .donorEmail(donation.getDonorEmail())
                .amount(amount)
                .amountFormatted(formatCurrency(amount))
                .currency(donation.getCurrency())
                .currencySymbol("£")
                .paymentStatus(donation.getPaymentStatus() != null ? donation.getPaymentStatus().name() : "PENDING")
                .paymentProvider(donation.getPaymentProvider() != null ? donation.getPaymentProvider() : "STRIPE")
                .providerPaymentId(donation.getProviderPaymentId() != null ? donation.getProviderPaymentId() : donation.getStripePaymentIntentId())
                .clientSecret(donation.getClientSecret())
                .lovedOneNames(lovedOneNames)
                .dedicationMessage(dedicationMessage)
                .receiptAvailable(receiptAvailable)
                .receiptNumber(receiptNumber)
                .createdAt(donation.getCreatedAt())
                .updatedAt(donation.getUpdatedAt())
                .build();
    }

    public com.rememberme.api.communityfund.dto.response.DonationReceiptInfoResponse toDonationReceiptInfoResponse(
            com.rememberme.api.entity.DonationReceipt receipt) {

        BigDecimal amount = penceToBigDecimal(receipt.getAmountPence());

        return com.rememberme.api.communityfund.dto.response.DonationReceiptInfoResponse.builder()
                .receiptNumber(receipt.getReceiptNumber())
                .donationId(receipt.getDonation() != null ? receipt.getDonation().getId() : null)
                .donorName(receipt.getDonorName())
                .donorEmail(receipt.getDonorEmail())
                .amount(amount)
                .amountFormatted(formatCurrency(amount))
                .currency(receipt.getCurrency())
                .paymentMethod(receipt.getPaymentMethod())
                .fundName(receipt.getFundName())
                .issuedAt(receipt.getIssuedAt())
                .receiptUrl(receipt.getReceiptUrl())
                .build();
    }
}
