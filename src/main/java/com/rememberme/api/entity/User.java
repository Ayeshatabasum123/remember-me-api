package com.rememberme.api.entity;
 
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_postcode_normalized", columnList = "postcode_normalized")
})
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
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String photoUrl;

    private String country;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.Check(constraints = "role IN ('USER', 'ADMIN', 'SUPER_ADMIN')")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Role role;

    private String postcode;

    @Column(name = "postcode_normalized")
    private String postcodeNormalized;

    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean enabled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Role {
        USER, ADMIN, SUPER_ADMIN
    }
}
