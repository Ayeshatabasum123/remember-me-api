package com.rememberme.api.service;

import com.rememberme.api.dto.response.DonationReceiptResponse;
import com.rememberme.api.entity.Donation;
import com.rememberme.api.entity.DonationReceipt;

public interface DonationReceiptService {

    /**
     * Issues an official receipt for a succeeded donation exactly once.
     * Idempotent: returns existing receipt if already generated.
     */
    DonationReceipt generateReceipt(Donation donation, String paymentMethod, String stripeChargeId, String receiptUrl);

    /**
     * Retrieves the receipt for a donation, verifying that the requesting user is the donor.
     */
    DonationReceiptResponse getReceiptForDonor(Long donationId, Long requestingUserId);
}
