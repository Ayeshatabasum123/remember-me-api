package com.rememberme.api.communityfund.service;

import com.rememberme.api.communityfund.dto.response.FundHomeResponse;
import com.rememberme.api.communityfund.dto.response.FundImpactResponse;
import com.rememberme.api.communityfund.dto.response.FundPublicBalanceSummaryResponse;

public interface FundHomeService {

    /**
     * Retrieves public informational profile, purpose, and disclaimers for the Community Burial Fund.
     */
    FundHomeResponse getFundHomeInfo();

    /**
     * Calculates and returns approved public impact statistics (confirmed donations, amounts, disbursements).
     * Strictly anonymized: does not expose donor names, applicant names, or case details.
     */
    FundImpactResponse getFundImpact();

    /**
     * Calculates and returns approved public balance summary figures.
     * Does not expose private ledger details or reserved allocations.
     */
    FundPublicBalanceSummaryResponse getPublicBalanceSummary();
}
