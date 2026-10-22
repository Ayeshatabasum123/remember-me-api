package com.rememberme.api.communityfund.dto.request;

import com.rememberme.api.communityfund.enums.SupportUrgency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for updating an editable support request (PENDING or MORE_INFO_REQUIRED)")
public class UpdateSupportRequestDto {

    @Schema(description = "Full name of the applicant", example = "Ahmed Khan")
    private String applicantName;

    @Email(message = "Email must be a valid email address")
    @Schema(description = "Applicant email address", example = "ahmed@example.com")
    private String email;

    @Schema(description = "Applicant telephone number", example = "+447700900123")
    private String phone;

    @Schema(description = "Relationship to deceased", example = "Son")
    private String relationshipToDeceased;

    @Schema(description = "Name of funeral director, cemetery, or burial services provider", example = "Example Funeral Services")
    private String burialProviderName;

    @Schema(description = "Phone or email contact of burial services provider", example = "+447700900456")
    private String burialProviderContact;

    @DecimalMin(value = "0.01", message = "Total burial cost must be greater than zero")
    @Schema(description = "Total estimated or invoiced burial cost in GBP", example = "3200.00")
    private BigDecimal totalCost;

    @Schema(description = "Other funding or family contributions already secured", example = "1000.00")
    private BigDecimal otherFunding;

    @DecimalMin(value = "0.01", message = "Requested assistance amount must be greater than zero")
    @Schema(description = "Requested assistance amount from Community Burial Fund", example = "2200.00")
    private BigDecimal requestedAmount;

    @Schema(description = "Urgency of burial assistance: LOW, MEDIUM, HIGH, CRITICAL", example = "HIGH")
    private SupportUrgency urgency;

    @Schema(description = "Confidential supporting context and hardship details", example = "Updated invoice amounts provided.")
    private String supportingNotes;
}
