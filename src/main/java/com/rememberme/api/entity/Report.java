package com.rememberme.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "grave_id", nullable = false)
    private Grave grave;

    @ManyToOne
    @JoinColumn(name = "reported_by_user_id")
    private User reportedBy;

    @Enumerated(EnumType.STRING)
    private IssueType issueType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    private LocalDateTime createdAt;

    public enum IssueType {
        WRONG_LOCATION, WRONG_DETAILS, DUPLICATE, INAPPROPRIATE_CONTENT, OTHER
    }

    public enum ReportStatus {
        OPEN, IN_REVIEW, RESOLVED, DISMISSED
    }
}
