package com.rememberme.api.service;

import com.rememberme.api.dto.request.CreateDonationPaymentIntentRequest;
import com.rememberme.api.dto.response.CreatePaymentIntentResponse;
import com.rememberme.api.dto.response.DonationOptionsResponse;
import com.rememberme.api.dto.response.DonationResponse;
import com.rememberme.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DonationService {

    /**
     * Returns pre-configured preset tiers, permitted custom amount limits,
     * GBP currency info, and shared-fund explanation.
     */
    DonationOptionsResponse getDonationOptions();

    /**
     * Validates amounts and dedication targets on server, creates pending donation,
     * calls Stripe to create/reuse PaymentIntent, and returns client secret for Flutter PaymentSheet.
     */
    CreatePaymentIntentResponse createPaymentIntent(User donor, CreateDonationPaymentIntentRequest request, String headerIdempotencyKey);

    /**
     * Returns paginated donation history strictly private to the authenticated donor.
     */
    Page<DonationResponse> getMyDonations(User donor, Pageable pageable);

    /**
     * Returns donation status and details, strictly enforcing donor ownership.
     */
    DonationResponse getMyDonationById(Long donationId, Long requestingUserId);
}
