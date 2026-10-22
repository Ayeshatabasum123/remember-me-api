package com.rememberme.api.communityfund.repository;

import com.rememberme.api.communityfund.entity.SupportRequest;
import com.rememberme.api.communityfund.enums.SupportRequestStatus;
import com.rememberme.api.communityfund.enums.SupportUrgency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long>, JpaSpecificationExecutor<SupportRequest> {

    Page<SupportRequest> findByApplicantIdOrderByCreatedAtDesc(Long applicantId, Pageable pageable);

    List<SupportRequest> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);

    Optional<SupportRequest> findByIdAndApplicantId(Long id, Long applicantId);

    Optional<SupportRequest> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    @Query("SELECT sr FROM SupportRequest sr WHERE " +
           "(:status IS NULL OR sr.status = :status) AND " +
           "(:urgency IS NULL OR sr.urgency = :urgency) AND " +
           "(:search IS NULL OR LOWER(sr.applicantName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(sr.burialProviderName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(sr.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<SupportRequest> findAdminRequestsWithFilters(
            @Param("status") SupportRequestStatus status,
            @Param("urgency") SupportUrgency urgency,
            @Param("search") String search,
            Pageable pageable
    );

    long countByStatus(SupportRequestStatus status);

    @Query("SELECT COUNT(sr) FROM SupportRequest sr WHERE sr.applicant.id = :applicantId AND sr.createdAt >= :since")
    long countRecentRequestsByApplicant(@Param("applicantId") Long applicantId, @Param("since") LocalDateTime since);
}
