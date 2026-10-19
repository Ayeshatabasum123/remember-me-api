package com.rememberme.api.repository;

import com.rememberme.api.entity.Donation;
import com.rememberme.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    Optional<Donation> findByStripePaymentIntentId(String stripePaymentIntentId);

    Optional<Donation> findByIdempotencyKey(String idempotencyKey);

    Optional<Donation> findByStripeChargeId(String stripeChargeId);

    Page<Donation> findByUser(User user, Pageable pageable);

    Page<Donation> findByUserId(Long userId, Pageable pageable);

    Optional<Donation> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(d) FROM Donation d WHERE d.settlementStatus = 'SETTLED'")
    long countSettledDonations();

    @Query("SELECT COALESCE(SUM(d.amountPence), 0) FROM Donation d WHERE d.settlementStatus = 'SETTLED'")
    long sumSettledGrossPence();

    @Query("SELECT COALESCE(SUM(d.feePence), 0) FROM Donation d WHERE d.settlementStatus = 'SETTLED'")
    long sumSettledFeesPence();

    @Query("SELECT COALESCE(SUM(d.refundedAmountPence), 0) FROM Donation d WHERE d.settlementStatus = 'SETTLED'")
    long sumSettledRefundsPence();
}
