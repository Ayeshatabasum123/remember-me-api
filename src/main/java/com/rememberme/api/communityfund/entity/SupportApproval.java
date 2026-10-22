package com.rememberme.api.communityfund.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rememberme.api.communityfund.enums.ApprovalDecision;
import com.rememberme.api.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_approvals", uniqueConstraints = {
    @UniqueConstraint(name = "uk_support_approvals_request_approver", columnNames = {"support_request_id", "approver_user_id"})
}, indexes = {
    @Index(name = "idx_support_approvals_request_id", columnList = "support_request_id"),
    @Index(name = "idx_support_approvals_approver", columnList = "approver_user_id"),
    @Index(name = "idx_support_approvals_decision", columnList = "decision"),
    @Index(name = "idx_support_approvals_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupportApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_request_id", nullable = false)
    @JsonIgnore
    private SupportRequest supportRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_user_id", nullable = false)
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApprovalDecision decision;

    @Column(name = "approved_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "decision_reason", nullable = false, columnDefinition = "TEXT")
    private String decisionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
