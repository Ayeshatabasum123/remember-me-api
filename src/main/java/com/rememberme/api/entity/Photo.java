package com.rememberme.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "photos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    private OwnerType ownerType;

    private Long ownerId;

    @ManyToOne
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedBy;

    private LocalDateTime createdAt;

    public enum OwnerType {
        GRAVE, MEMORIAL
    }
}
