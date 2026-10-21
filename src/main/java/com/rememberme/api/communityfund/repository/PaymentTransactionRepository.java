package com.rememberme.api.communityfund.repository;

import com.rememberme.api.communityfund.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByDonationIdOrderByCreatedAtDesc(Long donationId);

    List<PaymentTransaction> findByProviderTransactionId(String providerTransactionId);
}
