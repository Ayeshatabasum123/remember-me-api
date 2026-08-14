package com.rememberme.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "deceased_persons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeceasedPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    private LocalDate dateOfBirth;

    private LocalDate dateOfDeath;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String photoUrl;

    @ManyToOne
    @JoinColumn(name = "grave_id", nullable = false)
    private Grave grave;

    @ManyToOne
    @JoinColumn(name = "added_by_user_id")
    private User addedBy;

    private boolean duplicateChecked;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Gender {
        MALE, FEMALE, OTHER
    }
}
