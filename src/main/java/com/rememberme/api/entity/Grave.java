package com.rememberme.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "graves")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Grave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "graveyard_id", nullable = false)
    private RememberMe rememberMe;

    @Column(name = "grave_number")
    private String graveNumber;

    @Column(name = "postcode", length = 20)
    private String postcode;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private Double locationAccuracy;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum VerificationStatus {
        UNVERIFIED, VERIFIED, DISPUTED
    }
}
