package com.rememberme.api.service;

import com.rememberme.api.dto.response.FundSummaryResponse;
import com.rememberme.api.entity.Donation;
import com.rememberme.api.entity.FundLedger;

public interface FundLedgerService {

    /**
     * Records an immutable settlement entry for a confirmed donation once fees and balance transaction are known.
     */
    FundLedger recordDonationSettlement(Donation donation, Long feePence, String balanceTransactionId);

    /**
     * Records an immutable refund reversal entry in the fund ledger.
     */
    FundLedger recordDonationRefund(Donation donation, Long refundAmountPence, String refundId, String reason);

    /**
     * Records an immutable dispute/chargeback adjustment entry in the fund ledger.
     */
    FundLedger recordDisputeAdjustment(Donation donation, Long disputedAmountPence, Long disputeFeePence, String disputeId);

    /**
     * Returns the aggregated public summary of the Community Burial Fund.
     * Never exposes donor identities, dedications, or private assistance records.
     */
    FundSummaryResponse getFundSummary();

    /**
     * Returns current available fund balance in pence.
     */
    long getAvailableBalancePence();
}
