package com.rememberme.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "memorials", indexes = {
    @Index(name = "idx_memorial_deceased_person", columnList = "deceased_person_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Memorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "deceased_person_id", nullable = false)
    private DeceasedPerson deceasedPerson;

    @Column(columnDefinition = "TEXT")
    private String biography;

    @Column(columnDefinition = "TEXT")
    private String prayerOrMessage;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
