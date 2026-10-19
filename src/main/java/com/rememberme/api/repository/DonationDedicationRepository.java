package com.rememberme.api.repository;

import com.rememberme.api.entity.Donation;
import com.rememberme.api.entity.DonationDedication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationDedicationRepository extends JpaRepository<DonationDedication, Long> {
    List<DonationDedication> findByDonation(Donation donation);
    List<DonationDedication> findByDonationId(Long donationId);
}
