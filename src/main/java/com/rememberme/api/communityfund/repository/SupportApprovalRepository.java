package com.rememberme.api.communityfund.repository;

import com.rememberme.api.communityfund.entity.SupportApproval;
import com.rememberme.api.communityfund.enums.ApprovalDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportApprovalRepository extends JpaRepository<SupportApproval, Long> {

    List<SupportApproval> findBySupportRequestIdOrderByCreatedAtAsc(Long supportRequestId);

    Optional<SupportApproval> findBySupportRequestIdAndApproverId(Long supportRequestId, Long approverUserId);

    boolean existsBySupportRequestIdAndApproverId(Long supportRequestId, Long approverUserId);

    long countBySupportRequestIdAndDecision(Long supportRequestId, ApprovalDecision decision);
}
