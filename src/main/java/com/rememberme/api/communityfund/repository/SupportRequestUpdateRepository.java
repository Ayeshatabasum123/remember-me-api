package com.rememberme.api.communityfund.repository;

import com.rememberme.api.communityfund.entity.SupportRequestUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportRequestUpdateRepository extends JpaRepository<SupportRequestUpdate, Long> {

    List<SupportRequestUpdate> findBySupportRequestIdOrderByCreatedAtDesc(Long supportRequestId);

    Page<SupportRequestUpdate> findBySupportRequestIdOrderByCreatedAtDesc(Long supportRequestId, Pageable pageable);

    long countBySupportRequestId(Long supportRequestId);
}
