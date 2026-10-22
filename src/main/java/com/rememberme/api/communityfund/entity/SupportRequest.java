package com.rememberme.api.communityfund.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rememberme.api.communityfund.enums.SupportRequestStatus;
import com.rememberme.api.communityfund.enums.SupportUrgency;
import com.rememberme.api.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "support_requests", indexes = {
    @Index(name = "idx_support_requests_applicant_user_id", columnList = "applicant_user_id"),
    @Index(name = "idx_support_requests_status", columnList = "status"),
    @Index(name = "idx_support_requests_urgency", columnList = "urgency"),
    @Index(name = "idx_support_requests_created_at", columnList = "created_at"),
    @Index(name = "idx_support_requests_idempotency_key", columnList = "idempotency_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_user_id", nullable = false)
    private User applicant;

    @Column(name = "applicant_name", nullable = false, length = 255)
    private String applicantName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(name = "relationship_to_deceased", nullable = false, length = 100)
    private String relationshipToDeceased;

    @Column(name = "authority_to_apply", nullable = false)
    @Builder.Default
    private Boolean authorityToApply = true;

    @Column(name = "burial_provider_name", nullable = false, length = 255)
    private String burialProviderName;

    @Column(name = "burial_provider_contact", nullable = false, length = 100)
    private String burialProviderContact;

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "other_funding", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal otherFunding = BigDecimal.ZERO;

    @Column(name = "requested_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal requestedAmount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "GBP";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private SupportUrgency urgency = SupportUrgency.MEDIUM;

    @Column(name = "supporting_notes", columnDefinition = "TEXT")
    private String supportingNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private SupportRequestStatus status = SupportRequestStatus.PENDING;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    // Review and Verification fields (used in Module 6)
    @Column(name = "provider_verified", nullable = false)
    @Builder.Default
    private Boolean providerVerified = false;

    @Column(name = "provider_verification_notes", columnDefinition = "TEXT")
    private String providerVerificationNotes;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @OneToMany(mappedBy = "supportRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SupportDocument> documents = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (currency == null) {
            currency = "GBP";
        }
        if (urgency == null) {
            urgency = SupportUrgency.MEDIUM;
        }
        if (status == null) {
            status = SupportRequestStatus.PENDING;
        }
        if (otherFunding == null) {
            otherFunding = BigDecimal.ZERO;
        }
        if (authorityToApply == null) {
            authorityToApply = true;
        }
        if (providerVerified == null) {
            providerVerified = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
