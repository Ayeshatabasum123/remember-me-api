package com.rememberme.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "fund_ledger", indexes = {
    @Index(name = "idx_fund_ledger_donation_id", columnList = "donation_id"),
    @Index(name = "idx_fund_ledger_entry_type", columnList = "entry_type"),
    @Index(name = "idx_fund_ledger_created_at", columnList = "created_at"),
    @Index(name = "idx_fund_ledger_reference", columnList = "reference")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "donation_id")
    private Long donationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 50)
    private FundLedgerEntryType entryType;

    @Column(name = "gross_amount_pence", nullable = false)
    private Long grossAmountPence;

    @Column(name = "fee_pence", nullable = false)
    @Builder.Default
    private Long feePence = 0L;

    @Column(name = "net_amount_pence", nullable = false)
    private Long netAmountPence;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "GBP";

    @Column(name = "running_balance_pence", nullable = false)
    private Long runningBalancePence;

    @Column(name = "stripe_balance_transaction_id")
    private String stripeBalanceTransactionId;

    @Column(nullable = false, length = 255)
    private String reference;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (currency == null) {
            currency = "GBP";
        }
        if (feePence == null) {
            feePence = 0L;
        }
    }
}
