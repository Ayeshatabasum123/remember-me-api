package com.rememberme.api.repository;

import com.rememberme.api.entity.FundLedger;
import com.rememberme.api.entity.FundLedgerEntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FundLedgerRepository extends JpaRepository<FundLedger, Long> {

    Optional<FundLedger> findTopByOrderByIdDesc();

    List<FundLedger> findByDonationId(Long donationId);

    Optional<FundLedger> findByReference(String reference);

    boolean existsByReference(String reference);

    boolean existsByDonationIdAndEntryType(Long donationId, FundLedgerEntryType entryType);

    Page<FundLedger> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(fl.netAmountPence), 0) FROM FundLedger fl")
    long sumTotalAvailableBalancePence();

    @Query("SELECT COALESCE(SUM(fl.grossAmountPence), 0) FROM FundLedger fl WHERE fl.entryType = 'DONATION_SETTLEMENT'")
    long sumSettledDonationGrossPence();

    @Query("SELECT COALESCE(SUM(fl.feePence), 0) FROM FundLedger fl")
    long sumTotalFeesPence();

    @Query("SELECT COALESCE(SUM(ABS(fl.grossAmountPence)), 0) FROM FundLedger fl WHERE fl.entryType = 'DONATION_REFUND'")
    long sumTotalRefundsGrossPence();

    @Query("SELECT COUNT(fl) FROM FundLedger fl WHERE fl.entryType = 'DONATION_SETTLEMENT'")
    long countSettledDonationEntries();

    @Query("SELECT COUNT(fl) FROM FundLedger fl WHERE fl.entryType = 'BURIAL_SUPPORT_DISBURSEMENT'")
    long countDisbursementEntries();

    @Query("SELECT COALESCE(SUM(ABS(fl.netAmountPence)), 0) FROM FundLedger fl WHERE fl.entryType = 'BURIAL_SUPPORT_DISBURSEMENT'")
    long sumTotalDisbursementsPence();
}
