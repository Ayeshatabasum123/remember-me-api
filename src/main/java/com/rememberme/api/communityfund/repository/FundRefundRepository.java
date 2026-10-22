package com.rememberme.api.communityfund.repository;

import com.rememberme.api.communityfund.entity.FundRefund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FundRefundRepository extends JpaRepository<FundRefund, Long> {

    List<FundRefund> findByOriginalTransactionId(Long originalTransactionId);

    List<FundRefund> findByDonationId(Long donationId);

    Page<FundRefund> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(r.amountPence), 0) FROM FundRefund r WHERE r.status = 'COMPLETED'")
    long sumCompletedRefundsPence();

    @Query("SELECT COUNT(r) FROM FundRefund r WHERE r.status = 'COMPLETED'")
    long countCompletedRefunds();
}
