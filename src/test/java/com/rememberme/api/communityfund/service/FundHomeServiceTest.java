package com.rememberme.api.communityfund.service;

import com.rememberme.api.communityfund.config.CommunityFundProperties;
import com.rememberme.api.communityfund.dto.response.FundHomeResponse;
import com.rememberme.api.communityfund.dto.response.FundImpactResponse;
import com.rememberme.api.communityfund.dto.response.FundPublicBalanceSummaryResponse;
import com.rememberme.api.communityfund.entity.FundSettings;
import com.rememberme.api.communityfund.mapper.FundMapper;
import com.rememberme.api.communityfund.repository.FundSettingsRepository;
import com.rememberme.api.communityfund.service.impl.FundHomeServiceImpl;
import com.rememberme.api.repository.FundLedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FundHomeServiceTest {

    @Mock
    private FundSettingsRepository fundSettingsRepository;

    @Mock
    private FundLedgerRepository fundLedgerRepository;

    private CommunityFundProperties properties;
    private FundMapper fundMapper;
    private FundHomeServiceImpl fundHomeService;

    @BeforeEach
    void setUp() {
        properties = new CommunityFundProperties();
        fundMapper = new FundMapper();
        fundHomeService = new FundHomeServiceImpl(fundSettingsRepository, fundLedgerRepository, properties, fundMapper);
    }

    @Test
    @DisplayName("getFundHomeInfo returns database settings when configured")
    void testGetFundHomeInfo_WhenSettingsExist() {
        FundSettings settings = FundSettings.builder()
                .id(1L)
                .fundName("Remember Me Community Burial Fund")
                .introduction("Custom Intro")
                .purpose("Custom Purpose")
                .description("Custom Description")
                .donationInformation("Custom Donation Info")
                .privacyInformation("Custom Privacy Guarantee")
                .fundingDisclaimer("Custom Disclaimer")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(fundSettingsRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(settings));

        FundHomeResponse response = fundHomeService.getFundHomeInfo();

        assertThat(response).isNotNull();
        assertThat(response.getFundName()).isEqualTo("Remember Me Community Burial Fund");
        assertThat(response.getIntroduction()).isEqualTo("Custom Intro");
        assertThat(response.getPurpose()).isEqualTo("Custom Purpose");
        assertThat(response.getPrivacyInformation()).isEqualTo("Custom Privacy Guarantee");
    }

    @Test
    @DisplayName("getFundHomeInfo returns robust default settings when table is empty")
    void testGetFundHomeInfo_WhenSettingsEmpty_FallbackProvided() {
        when(fundSettingsRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());

        FundHomeResponse response = fundHomeService.getFundHomeInfo();

        assertThat(response).isNotNull();
        assertThat(response.getFundName()).isEqualTo("Remember Me Community Burial Fund");
        assertThat(response.getPurpose()).isNotBlank();
        assertThat(response.getPrivacyInformation()).isNotBlank();
        assertThat(response.getFundingDisclaimer()).isNotBlank();
    }

    @Test
    @DisplayName("getFundImpact accurately aggregates confirmed donations and disbursements from ledger")
    void testGetFundImpact_CalculatesFromLedger() {
        when(fundLedgerRepository.countSettledDonationEntries()).thenReturn(50L);
        when(fundLedgerRepository.sumSettledDonationGrossPence()).thenReturn(250000L); // £2,500.00
        when(fundLedgerRepository.countDisbursementEntries()).thenReturn(3L);
        when(fundLedgerRepository.sumTotalDisbursementsPence()).thenReturn(150000L); // £1,500.00

        FundImpactResponse response = fundHomeService.getFundImpact();

        assertThat(response).isNotNull();
        assertThat(response.getCurrency()).isEqualTo("GBP");
        assertThat(response.getCurrencySymbol()).isEqualTo("£");
        assertThat(response.getTotalConfirmedDonations()).isEqualTo(50L);
        assertThat(response.getTotalAmountReceived()).isEqualByComparingTo(BigDecimal.valueOf(2500.00));
        assertThat(response.getTotalAmountReceivedFormatted()).contains("2,500.00");
        assertThat(response.getTotalCompletedSupportCases()).isEqualTo(3L);
        assertThat(response.getTotalApprovedSupportDisbursed()).isEqualByComparingTo(BigDecimal.valueOf(1500.00));
        assertThat(response.getTotalApprovedSupportDisbursedFormatted()).contains("1,500.00");
    }

    @Test
    @DisplayName("getPublicBalanceSummary returns approved public figures without exposing private ledger items")
    void testGetPublicBalanceSummary_CalculatesApprovedPublicValues() {
        when(fundLedgerRepository.countSettledDonationEntries()).thenReturn(100L);
        when(fundLedgerRepository.sumSettledDonationGrossPence()).thenReturn(1000000L); // £10,000.00
        when(fundLedgerRepository.sumTotalDisbursementsPence()).thenReturn(400000L); // £4,000.00
        when(fundLedgerRepository.sumTotalAvailableBalancePence()).thenReturn(580000L); // £5,800.00 (net after fees/disbursements)

        FundPublicBalanceSummaryResponse response = fundHomeService.getPublicBalanceSummary();

        assertThat(response).isNotNull();
        assertThat(response.getCurrency()).isEqualTo("GBP");
        assertThat(response.getTotalConfirmedDonations()).isEqualTo(100L);
        assertThat(response.getTotalAmountReceived()).isEqualByComparingTo(BigDecimal.valueOf(10000.00));
        assertThat(response.getTotalApprovedSupportDisbursed()).isEqualByComparingTo(BigDecimal.valueOf(4000.00));
        assertThat(response.getAvailableBalance()).isEqualByComparingTo(BigDecimal.valueOf(5800.00));
        assertThat(response.getAvailableBalanceFormatted()).contains("5,800.00");
    }
}
