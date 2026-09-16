package com.rememberme.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "funeral_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuneralEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deceased_person_id", nullable = false)
    private DeceasedPerson deceasedPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graveyard_id")
    private RememberMe graveyard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Religion religion;

    @Column(nullable = false)
    private LocalDateTime funeralDateTime;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    @Builder.Default
    private boolean isVip = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isNational = false;

    private String targetCountry;
    private String targetRegion;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FuneralStatus status = FuneralStatus.SCHEDULED;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum FuneralStatus {
        SCHEDULED, ONGOING, COMPLETED, CANCELLED
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
