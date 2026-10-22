package com.rememberme.api.communityfund.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rememberme.api.entity.Donation;
import com.rememberme.api.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds", indexes = {
    @Index(name = "idx_refunds_original_tx", columnList = "original_transaction_id"),
    @Index(name = "idx_refunds_donation_id", columnList = "donation_id"),
    @Index(name = "idx_refunds_status", columnList = "status"),
    @Index(name = "idx_refunds_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_transaction_id", nullable = false)
    private Long originalTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id")
    private Donation donation;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "amount_pence", nullable = false)
    private Long amountPence;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "GBP";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "COMPLETED";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by", nullable = false)
    private User processedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (completedAt == null && "COMPLETED".equals(status)) {
            completedAt = now;
        }
        if (currency == null) {
            currency = "GBP";
        }
        if (amountPence == null && amount != null) {
            amountPence = amount.multiply(BigDecimal.valueOf(100)).longValue();
        }
    }
}
