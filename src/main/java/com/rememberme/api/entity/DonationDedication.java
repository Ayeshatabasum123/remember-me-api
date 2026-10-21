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
@Table(name = "donation_dedications", indexes = {
    @Index(name = "idx_donation_dedications_donation_id", columnList = "donation_id"),
    @Index(name = "idx_donation_dedications_deceased_id", columnList = "deceased_person_id"),
    @Index(name = "idx_donation_dedications_memorial_id", columnList = "memorial_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DonationDedication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Donation donation;

    @Column(name = "dedication_name", nullable = false)
    private String dedicationName;

    @Column(name = "loved_one_name")
    private String lovedOneName;

    @Column(name = "deceased_person_id")
    private Long deceasedPersonId;

    @Column(name = "memorial_id")
    private Long memorialId;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "dedication_message", columnDefinition = "TEXT")
    private String dedicationMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (lovedOneName == null && dedicationName != null) {
            lovedOneName = dedicationName;
        }
        if (dedicationName == null && lovedOneName != null) {
            dedicationName = lovedOneName;
        }
        if (dedicationMessage == null && message != null) {
            dedicationMessage = message;
        }
        if (message == null && dedicationMessage != null) {
            message = dedicationMessage;
        }
    }
}
