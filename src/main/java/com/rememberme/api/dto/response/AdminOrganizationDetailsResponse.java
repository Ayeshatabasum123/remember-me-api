package com.rememberme.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rememberme.api.entity.Organization;
import com.rememberme.api.entity.OrganizationApprovalStatus;
import com.rememberme.api.entity.OrganizationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Details of the religious organization associated with the authenticated admin")
public class AdminOrganizationDetailsResponse {

    @Schema(description = "Unique database ID of the organization", example = "2")
    private Long organizationId;

    @Schema(description = "Name of the religious organization", example = "Westminster Community Church")
    private String organizationName;

    @Schema(description = "Unique organization code issued upon approval", example = "ORG-000002")
    private String organizationCode;

    @Schema(description = "Type of religious organization (CHURCH, MOSQUE, TEMPLE, OTHER)", example = "CHURCH")
    private OrganizationType organizationType;

    @Schema(description = "UK postcode of the organization", example = "SW1A 1AA")
    private String postcode;

    @Schema(description = "Approval status of the organization (PENDING, APPROVED, REJECTED)", example = "APPROVED")
    private OrganizationApprovalStatus approvalStatus;

    public static AdminOrganizationDetailsResponse fromEntity(Organization org) {
        if (org == null) {
            return null;
        }
        return AdminOrganizationDetailsResponse.builder()
                .organizationId(org.getId())
                .organizationName(org.getName())
                .organizationCode(org.getOrganizationCode())
                .organizationType(org.getOrganizationType())
                .postcode(org.getPostcode())
                .approvalStatus(org.getApprovalStatus())
                .build();
    }
}
