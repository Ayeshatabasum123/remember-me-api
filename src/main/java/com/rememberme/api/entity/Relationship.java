package com.rememberme.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "relationships")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Relationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "deceased_person_id", nullable = false)
    private DeceasedPerson deceasedPerson;

    @Enumerated(EnumType.STRING)
    private RelationType relationType;

    private LocalDateTime createdAt;

    public enum RelationType {
        FATHER, MOTHER, SPOUSE, SON, DAUGHTER, SIBLING, FRIEND, RELATIVE, OTHER
    }
}
