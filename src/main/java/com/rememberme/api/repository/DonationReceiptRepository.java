package com.rememberme.api.repository;

import com.rememberme.api.entity.Donation;
import com.rememberme.api.entity.DonationReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DonationReceiptRepository extends JpaRepository<DonationReceipt, Long> {
    Optional<DonationReceipt> findByDonation(Donation donation);
    Optional<DonationReceipt> findByDonationId(Long donationId);
    Optional<DonationReceipt> findByReceiptNumber(String receiptNumber);
    boolean existsByDonationId(Long donationId);
}
