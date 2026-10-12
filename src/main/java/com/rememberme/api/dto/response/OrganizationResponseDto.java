package com.rememberme.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rememberme.api.entity.Organization;
import com.rememberme.api.entity.OrganizationApprovalStatus;
import com.rememberme.api.entity.OrganizationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationResponseDto {
    private Long id;
    private String name;
    private OrganizationType organizationType;
    private String email;
    private String phone;
    private String postcode;
    private String normalizedPostcode;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private String organizationCode;
    private OrganizationApprovalStatus approvalStatus;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrganizationResponseDto fromEntity(Organization org) {
        if (org == null) return null;
        return OrganizationResponseDto.builder()
                .id(org.getId())
                .name(org.getName())
                .organizationType(org.getOrganizationType())
                .email(org.getEmail())
                .phone(org.getPhone())
                .postcode(org.getPostcode())
                .normalizedPostcode(org.getPostcodeNormalized())
                .latitude(org.getLatitude())
                .longitude(org.getLongitude())
                .photoUrl(org.getPhotoUrl())
                .organizationCode(org.getOrganizationCode())
                .approvalStatus(org.getApprovalStatus())
                .rejectionReason(org.getRejectionReason())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }
}
