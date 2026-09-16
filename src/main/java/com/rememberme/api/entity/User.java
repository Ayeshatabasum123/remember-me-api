package com.rememberme.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(nullable = false)
    private String password;

    private String photoUrl;

    private String country;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.Check(constraints = "role IN ('USER', 'ADMIN', 'GRAVEYARD_ADMIN', 'SUPER_ADMIN')")
    private Role role;

    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean enabled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Role {
        USER, ADMIN, GRAVEYARD_ADMIN, SUPER_ADMIN
    }
}
