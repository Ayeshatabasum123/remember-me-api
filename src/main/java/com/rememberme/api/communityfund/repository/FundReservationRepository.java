package com.rememberme.api.communityfund.repository;

import com.rememberme.api.communityfund.entity.FundReservation;
import com.rememberme.api.communityfund.enums.FundReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FundReservationRepository extends JpaRepository<FundReservation, Long> {

    Optional<FundReservation> findBySupportRequestIdAndReservationStatus(Long supportRequestId, FundReservationStatus status);

    boolean existsBySupportRequestIdAndReservationStatus(Long supportRequestId, FundReservationStatus status);

    List<FundReservation> findBySupportRequestId(Long supportRequestId);

    List<FundReservation> findByReservationStatus(FundReservationStatus status);

    Page<FundReservation> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<FundReservation> findByReservationStatusOrderByCreatedAtDesc(FundReservationStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(fr.amountPence), 0) FROM FundReservation fr WHERE fr.reservationStatus = 'RESERVED'")
    long sumActiveReservationsPence();
}
