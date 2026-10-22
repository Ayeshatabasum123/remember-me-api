package com.rememberme.api.communityfund.dto.response;

import com.rememberme.api.communityfund.enums.SupportRequestStatus;
import com.rememberme.api.communityfund.enums.SupportUrgency;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed representation of a Community Burial Fund support request")
public class SupportRequestResponseDto {

    @Schema(description = "Support request ID", example = "1")
    private Long id;

    @Schema(description = "Applicant user ID", example = "5")
    private Long applicantUserId;

    @Schema(description = "Applicant full name", example = "Ahmed Khan")
    private String applicantName;

    @Schema(description = "Applicant email", example = "ahmed@example.com")
    private String email;

    @Schema(description = "Applicant contact phone", example = "+447700900123")
    private String phone;

    @Schema(description = "Relationship to deceased", example = "Son")
    private String relationshipToDeceased;

    @Schema(description = "Authority to apply declaration", example = "true")
    private Boolean authorityToApply;

    @Schema(description = "Burial service provider name", example = "Example Funeral Services")
    private String burialProviderName;

    @Schema(description = "Burial service provider contact", example = "+447700900456")
    private String burialProviderContact;

    @Schema(description = "Total cost of burial in GBP", example = "3000.00")
    private BigDecimal totalCost;

    @Schema(description = "Other funding or family contributions in GBP", example = "1000.00")
    private BigDecimal otherFunding;

    @Schema(description = "Eligible outstanding cost (totalCost - otherFunding)", example = "2000.00")
    private BigDecimal eligibleOutstandingCost;

    @Schema(description = "Requested assistance amount in GBP", example = "2000.00")
    private BigDecimal requestedAmount;

    @Schema(description = "Currency", example = "GBP")
    private String currency;

    @Schema(description = "Urgency level", example = "HIGH")
    private SupportUrgency urgency;

    @Schema(description = "Confidential supporting context", example = "Financial assistance required for burial expenses.")
    private String supportingNotes;

    @Schema(description = "Current lifecycle status", example = "PENDING")
    private SupportRequestStatus status;

    @Schema(description = "Whether burial provider has been independently verified", example = "false")
    private Boolean providerVerified;

    @Schema(description = "List of uploaded supporting documents")
    private List<SupportDocumentResponseDto> documents;

    @Schema(description = "Submission timestamp", example = "2026-10-22T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2026-10-22T10:30:00")
    private LocalDateTime updatedAt;
}
