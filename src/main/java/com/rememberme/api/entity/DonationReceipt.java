package com.rememberme.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "donation_receipts", indexes = {
    @Index(name = "idx_donation_receipts_donation_id", columnList = "donation_id", unique = true),
    @Index(name = "idx_donation_receipts_receipt_number", columnList = "receipt_number", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DonationReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id", nullable = false, unique = true)
    @JsonIgnore
    @ToString.Exclude
    private Donation donation;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 64)
    private String receiptNumber;

    @Column(name = "donor_name", nullable = false)
    private String donorName;

    @Column(name = "donor_email", nullable = false)
    private String donorEmail;

    @Column(name = "amount_pence", nullable = false)
    private Long amountPence;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "GBP";

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "stripe_charge_id")
    private String stripeChargeId;

    @Column(name = "fund_name", nullable = false)
    @Builder.Default
    private String fundName = "Community Burial Fund";

    @Column(name = "receipt_url", length = 512)
    private String receiptUrl;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @PrePersist
    public void prePersist() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
        if (fundName == null) {
            fundName = "Community Burial Fund";
        }
        if (currency == null) {
            currency = "GBP";
        }
    }
}
