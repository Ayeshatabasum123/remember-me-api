package com.rememberme.api.communityfund.repository;

import com.rememberme.api.communityfund.entity.SupportDocument;
import com.rememberme.api.communityfund.enums.SupportDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportDocumentRepository extends JpaRepository<SupportDocument, Long> {

    List<SupportDocument> findBySupportRequestIdOrderByUploadedAtAsc(Long supportRequestId);

    Optional<SupportDocument> findByIdAndSupportRequestId(Long id, Long supportRequestId);

    boolean existsBySupportRequestIdAndDocumentTypeAndFileHash(Long supportRequestId, SupportDocumentType documentType, String fileHash);

    boolean existsByFileHashAndDocumentType(String fileHash, SupportDocumentType documentType);

    long countBySupportRequestId(Long supportRequestId);
}
