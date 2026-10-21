package com.rememberme.api.communityfund.service.impl;

import com.rememberme.api.communityfund.config.CommunityFundProperties;
import com.rememberme.api.communityfund.dto.response.FundHomeResponse;
import com.rememberme.api.communityfund.dto.response.FundImpactResponse;
import com.rememberme.api.communityfund.dto.response.FundPublicBalanceSummaryResponse;
import com.rememberme.api.communityfund.entity.FundSettings;
import com.rememberme.api.communityfund.mapper.FundMapper;
import com.rememberme.api.communityfund.repository.FundSettingsRepository;
import com.rememberme.api.communityfund.service.FundHomeService;
import com.rememberme.api.repository.FundLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FundHomeServiceImpl implements FundHomeService {

    private final FundSettingsRepository fundSettingsRepository;
    private final FundLedgerRepository fundLedgerRepository;
    private final CommunityFundProperties properties;
    private final FundMapper fundMapper;

    @Override
    @Transactional(readOnly = true)
    public FundHomeResponse getFundHomeInfo() {
        FundSettings settings = fundSettingsRepository.findTopByOrderByIdAsc()
                .orElseGet(this::createDefaultSettingsFallback);

        return fundMapper.toFundHomeResponse(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public FundImpactResponse getFundImpact() {
        long confirmedDonations = fundLedgerRepository.countSettledDonationEntries();
        long grossPence = fundLedgerRepository.sumSettledDonationGrossPence();
        long completedCases = fundLedgerRepository.countDisbursementEntries();
        long disbursedPence = fundLedgerRepository.sumTotalDisbursementsPence();

        return fundMapper.toFundImpactResponse(
                confirmedDonations,
                grossPence,
                completedCases,
                disbursedPence,
                properties.getCurrency(),
                properties.getCurrencySymbol()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public FundPublicBalanceSummaryResponse getPublicBalanceSummary() {
        long confirmedDonations = fundLedgerRepository.countSettledDonationEntries();
        long grossPence = fundLedgerRepository.sumSettledDonationGrossPence();
        long disbursedPence = fundLedgerRepository.sumTotalDisbursementsPence();
        long availableBalancePence = fundLedgerRepository.sumTotalAvailableBalancePence();

        return fundMapper.toPublicBalanceSummaryResponse(
                confirmedDonations,
                grossPence,
                disbursedPence,
                availableBalancePence,
                properties.getCurrency(),
                properties.getCurrencySymbol()
        );
    }

    private FundSettings createDefaultSettingsFallback() {
        return FundSettings.builder()
                .id(1L)
                .fundName("Remember Me Community Burial Fund")
                .introduction("Providing compassionate, communal burial assistance to families facing financial hardship.")
                .purpose("To relieve burial and funeral poverty by providing direct, discreet financial assistance to verified burial service providers on behalf of bereaved families in need.")
                .description("The Remember Me Community Burial Fund is a collective community initiative dedicated to ensuring every individual receives a dignified burial regardless of financial hardship. All contributions flow directly into a central, strictly audited communal fund.")
                .donationInformation("Donations of any amount are welcomed via card, Apple Pay, and Google Pay. You may dedicate your contribution in loving memory of deceased relatives, which serves as a tribute and prayer.")
                .privacyInformation("Strict applicant privacy is guaranteed by policy and system architecture. Financial hardship cases, family identities, applicant names, and documents are kept strictly confidential and are never published on memorials, public grave registries, or public impact dashboards.")
                .fundingDisclaimer("Financial assistance is subject to fund availability, independent verification of burial costs, and two-person administrative approval. An application does not guarantee funding.")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
