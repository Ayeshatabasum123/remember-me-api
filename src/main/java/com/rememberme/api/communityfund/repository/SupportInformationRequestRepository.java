package com.rememberme.api.communityfund.repository;

import com.rememberme.api.communityfund.entity.SupportInformationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportInformationRequestRepository extends JpaRepository<SupportInformationRequest, Long> {

    List<SupportInformationRequest> findBySupportRequestIdOrderByRequestedAtDesc(Long supportRequestId);

    Optional<SupportInformationRequest> findFirstBySupportRequestIdAndStatusOrderByRequestedAtDesc(Long supportRequestId, String status);

    long countBySupportRequestIdAndStatus(Long supportRequestId, String status);
}
