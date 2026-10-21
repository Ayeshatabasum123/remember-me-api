package com.rememberme.api.communityfund.service;

import com.rememberme.api.communityfund.dto.request.DonationCheckoutRequest;
import com.rememberme.api.communityfund.dto.response.*;
import com.rememberme.api.entity.User;

public interface CommunityDonationService {

    /**
     * Returns predefined donation amounts (£5, £10, £50) and custom amount bounds.
     */
    DonationAmountsResponse getDonationAmounts();

    /**
     * Creates a pending donation, attaches optional loved-one dedications,
     * initiates checkout with the configured payment provider (Stripe),
     * and logs the initial transaction.
     */
    DonationCheckoutResponse createCheckout(User donor, DonationCheckoutRequest request, String idempotencyKey);

    /**
     * Retrieves donation details and payment status, enforcing donor or admin authorization.
     */
    DonationDetailsResponse getDonationDetails(Long donationId, User user);

    /**
     * Retrieves official receipt information for a confirmed successful donation,
     * restricted strictly to the donor owner or authorized staff.
     */
    DonationReceiptInfoResponse getDonationReceipt(Long donationId, User user);

    /**
     * Safely retries a failed or expired payment, creating a refreshed payment intent
     * without creating duplicate successful donations or double counting.
     */
    DonationRetryResponse retryDonation(Long donationId, User user);

    /**
     * Retrieves paginated donation history strictly private to the authenticated user.
     */
    org.springframework.data.domain.Page<DonationDetailsResponse> getMyDonations(User user, int page, int size, String sortBy, String direction);
}
