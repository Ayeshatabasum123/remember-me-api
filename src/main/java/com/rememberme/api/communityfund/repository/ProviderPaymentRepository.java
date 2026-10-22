package com.rememberme.api.communityfund.repository;

import com.rememberme.api.communityfund.entity.ProviderPayment;
import com.rememberme.api.communityfund.enums.ProviderPaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderPaymentRepository extends JpaRepository<ProviderPayment, Long> {

    Optional<ProviderPayment> findByPaymentReference(String paymentReference);

    boolean existsByPaymentReference(String paymentReference);

    boolean existsBySupportRequestIdAndStatusIn(Long supportRequestId, List<ProviderPaymentStatus> statuses);

    List<ProviderPayment> findBySupportRequestId(Long supportRequestId);

    Page<ProviderPayment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ProviderPayment> findByStatusOrderByCreatedAtDesc(ProviderPaymentStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(pp.amountPence), 0) FROM ProviderPayment pp WHERE pp.status = 'PAID'")
    long sumTotalDisbursedPence();

    @Query("SELECT COUNT(pp) FROM ProviderPayment pp WHERE pp.status = 'PAID'")
    long countDisbursedPayments();
}
