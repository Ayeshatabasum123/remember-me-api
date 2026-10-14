package com.rememberme.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@Schema(description = "Authentication response containing JWT token, user details, and organization info for Admins")
public class AuthResponse {
    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "JWT Bearer access token")
    private String token;

    @Schema(description = "User email address", example = "admin@church.org")
    private String email;

    @Schema(description = "User full name or organization name", example = "Westminster Community Church")
    private String fullName;

    @Schema(description = "User role", example = "ADMIN")
    private String role;

    @Schema(description = "User summary profile")
    private UserSummary user;

    @Schema(description = "Name of organization (for Admin login)", example = "Westminster Community Church")
    private String organizationName;

    @Schema(description = "Unique organization code (for Admin login)", example = "ORG-000002")
    private String organizationCode;

    @Schema(description = "Organization postcode (for Admin login)", example = "SW1A 1AA")
    private String postcode;

    @Schema(description = "Organization approval status (for Admin login)", example = "APPROVED")
    private String approvalStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Long id;
        private String fullName;
        private String email;
        private String role;
    }
}
