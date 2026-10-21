package com.rememberme.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "donations", indexes = {
    @Index(name = "idx_donations_user_id", columnList = "user_id"),
    @Index(name = "idx_donations_stripe_pi", columnList = "stripe_payment_intent_id", unique = true),
    @Index(name = "idx_donations_idempotency_key", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_donations_payment_status", columnList = "payment_status"),
    @Index(name = "idx_donations_settlement_status", columnList = "settlement_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(name = "amount_pence", nullable = false)
    private Long amountPence;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "GBP";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 32)
    @Builder.Default
    private DonationPaymentStatus paymentStatus = DonationPaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", nullable = false, length = 32)
    @Builder.Default
    private DonationSettlementStatus settlementStatus = DonationSettlementStatus.PENDING;

    @Column(name = "fee_pence", nullable = false)
    @Builder.Default
    private Long feePence = 0L;

    @Column(name = "net_amount_pence", nullable = false)
    @Builder.Default
    private Long netAmountPence = 0L;

    @Column(name = "refunded_amount_pence", nullable = false)
    @Builder.Default
    private Long refundedAmountPence = 0L;

    @Column(name = "stripe_payment_intent_id", unique = true)
    private String stripePaymentIntentId;

    @Column(name = "stripe_charge_id")
    private String stripeChargeId;

    @Column(name = "stripe_balance_transaction_id")
    private String stripeBalanceTransactionId;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "client_secret", length = 512)
    private String clientSecret;

    @Column(name = "payment_provider", length = 50)
    @Builder.Default
    private String paymentProvider = "STRIPE";

    @Column(name = "provider_payment_id")
    private String providerPaymentId;

    @Column(name = "donor_email")
    private String donorEmail;

    @Column(name = "donor_name")
    private String donorName;

    @OneToMany(mappedBy = "donation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<DonationDedication> dedications = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "succeeded_at")
    private LocalDateTime succeededAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (currency == null) {
            currency = "GBP";
        }
        if (feePence == null) {
            feePence = 0L;
        }
        if (netAmountPence == null) {
            netAmountPence = 0L;
        }
        if (refundedAmountPence == null) {
            refundedAmountPence = 0L;
        }
        if (paymentProvider == null) {
            paymentProvider = "STRIPE";
        }
        if (providerPaymentId == null && stripePaymentIntentId != null) {
            providerPaymentId = stripePaymentIntentId;
        }
        if (stripePaymentIntentId == null && providerPaymentId != null) {
            stripePaymentIntentId = providerPaymentId;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (providerPaymentId == null && stripePaymentIntentId != null) {
            providerPaymentId = stripePaymentIntentId;
        }
        if (stripePaymentIntentId == null && providerPaymentId != null) {
            stripePaymentIntentId = providerPaymentId;
        }
    }
}
