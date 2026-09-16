package com.rememberme.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_notification_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private boolean nearbyFuneralsEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Double nearbyRadiusKm = 10.0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_preferred_religions", joinColumns = @JoinColumn(name = "preference_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "religion")
    @Builder.Default
    private Set<Religion> religions = new HashSet<>(Set.of(Religion.ALL));

    @Column(nullable = false)
    @Builder.Default
    private boolean famousGravesEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean vipFuneralsEnabled = true;

    private Double lastKnownLatitude;
    private Double lastKnownLongitude;
    private LocalDateTime lastLocationUpdate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
