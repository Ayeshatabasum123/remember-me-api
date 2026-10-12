package com.rememberme.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRegisterResponse {
    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private OrganizationResponseDto organization;
    private String message;
}
