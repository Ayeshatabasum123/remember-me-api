package com.rememberme.api.communityfund.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rememberme.api.communityfund.enums.ProviderPaymentStatus;
import com.rememberme.api.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "provider_payments", indexes = {
    @Index(name = "idx_provider_payments_request_id", columnList = "support_request_id"),
    @Index(name = "idx_provider_payments_reservation_id", columnList = "reservation_id"),
    @Index(name = "idx_provider_payments_reference", columnList = "payment_reference", unique = true),
    @Index(name = "idx_provider_payments_status", columnList = "status"),
    @Index(name = "idx_provider_payments_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_request_id", nullable = false)
    private SupportRequest supportRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private FundReservation reservation;

    @Column(name = "provider_name", nullable = false, length = 255)
    private String providerName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "amount_pence", nullable = false)
    private Long amountPence;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "GBP";

    @Column(name = "payment_reference", nullable = false, unique = true, length = 255)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private ProviderPaymentStatus status = ProviderPaymentStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by", nullable = false)
    private User initiatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (currency == null) {
            currency = "GBP";
        }
        if (status == null) {
            status = ProviderPaymentStatus.PENDING;
        }
        if (amountPence == null && amount != null) {
            amountPence = amount.multiply(BigDecimal.valueOf(100)).longValue();
        }
    }
}
