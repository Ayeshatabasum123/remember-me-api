package com.rememberme.api.communityfund.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rememberme.api.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_information_requests", indexes = {
    @Index(name = "idx_support_info_requests_request_id", columnList = "support_request_id"),
    @Index(name = "idx_support_info_requests_status", columnList = "status"),
    @Index(name = "idx_support_info_requests_requested_at", columnList = "requested_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupportInformationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_request_id", nullable = false)
    @JsonIgnore
    private SupportRequest supportRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(name = "information_required", nullable = false, columnDefinition = "TEXT")
    private String informationRequired;

    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private String status = "PENDING"; // PENDING, RESPONDED, RESOLVED

    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @PrePersist
    public void prePersist() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
    }
}
