package com.rememberme.api.communityfund.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "fund_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fund_name", nullable = false)
    private String fundName;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String purpose;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "donation_information", columnDefinition = "TEXT")
    private String donationInformation;

    @Column(name = "privacy_information", nullable = false, columnDefinition = "TEXT")
    private String privacyInformation;

    @Column(name = "funding_disclaimer", nullable = false, columnDefinition = "TEXT")
    private String fundingDisclaimer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
