package com.rememberme.api.communityfund.dto.request;

import com.rememberme.api.communityfund.enums.SupportUrgency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for creating a Community Burial Fund private financial support request")
public class CreateSupportRequestDto {

    @NotBlank(message = "Applicant name is required")
    @Schema(description = "Full name of the applicant", example = "Ahmed Khan")
    private String applicantName;

    @NotBlank(message = "Email address is required")
    @Email(message = "Email must be a valid email address")
    @Schema(description = "Applicant email address", example = "ahmed@example.com")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Schema(description = "Applicant telephone number", example = "+447700900123")
    private String phone;

    @NotBlank(message = "Relationship to deceased is required")
    @Schema(description = "Relationship to deceased (e.g. Son, Daughter, Spouse, Sibling, Legal Representative)", example = "Son")
    private String relationshipToDeceased;

    @NotNull(message = "Authority to apply declaration is required")
    @AssertTrue(message = "Authority to apply must be confirmed")
    @Schema(description = "Confirmation that applicant possesses legal authority or family consent to apply", example = "true")
    private Boolean authorityToApply;

    @NotBlank(message = "Burial provider name is required")
    @Schema(description = "Name of funeral director, cemetery, or burial services provider", example = "Example Funeral Services")
    private String burialProviderName;

    @NotBlank(message = "Burial provider contact is required")
    @Schema(description = "Phone or email contact of burial services provider", example = "+447700900456")
    private String burialProviderContact;

    @NotNull(message = "Total cost is required")
    @DecimalMin(value = "0.01", message = "Total burial cost must be greater than zero")
    @Schema(description = "Total estimated or invoiced burial cost in GBP", example = "3000.00")
    private BigDecimal totalCost;

    @Schema(description = "Other funding or family contributions already secured (default: 0.00)", example = "1000.00")
    private BigDecimal otherFunding;

    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "0.01", message = "Requested assistance amount must be greater than zero")
    @Schema(description = "Requested assistance amount from Community Burial Fund", example = "2000.00")
    private BigDecimal requestedAmount;

    @Schema(description = "Currency of the application (must be GBP for initial release)", example = "GBP")
    private String currency;

    @Schema(description = "Urgency of burial assistance: LOW, MEDIUM, HIGH, CRITICAL", example = "HIGH")
    private SupportUrgency urgency;

    @Schema(description = "Confidential supporting context and hardship details", example = "Financial assistance required for burial expenses.")
    private String supportingNotes;
}
