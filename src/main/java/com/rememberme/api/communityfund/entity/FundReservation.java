package com.rememberme.api.communityfund.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rememberme.api.communityfund.enums.FundReservationStatus;
import com.rememberme.api.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fund_reservations", indexes = {
    @Index(name = "idx_fund_reservations_request_id", columnList = "support_request_id"),
    @Index(name = "idx_fund_reservations_status", columnList = "reservation_status"),
    @Index(name = "idx_fund_reservations_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_request_id", nullable = false)
    private SupportRequest supportRequest;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "amount_pence", nullable = false)
    private Long amountPence;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false, length = 32)
    @Builder.Default
    private FundReservationStatus reservationStatus = FundReservationStatus.RESERVED;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_by", nullable = false)
    private User reservedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "released_by")
    private User releasedBy;

    @Column(name = "release_reason", columnDefinition = "TEXT")
    private String releaseReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (reservationStatus == null) {
            reservationStatus = FundReservationStatus.RESERVED;
        }
        if (amountPence == null && amount != null) {
            amountPence = amount.multiply(BigDecimal.valueOf(100)).longValue();
        }
    }
}
