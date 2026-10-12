package com.rememberme.api.controller;

import com.rememberme.api.dto.request.RejectOrganizationRequest;
import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.dto.response.OrganizationResponseDto;
import com.rememberme.api.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Super Admin", description = "Review and approve or reject religious organization registrations")
public class SuperAdminController {

    private final OrganizationService organizationService;

    @GetMapping("/organizations/pending")
    @Operation(summary = "Retrieve organizations waiting for approval", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<List<OrganizationResponseDto>> getPendingOrganizations() {
        List<OrganizationResponseDto> pending = organizationService.getPendingOrganizations();
        return ApiResponse.success("Pending organizations retrieved successfully", pending);
    }

    @PutMapping("/organizations/{id}/approve")
    @Operation(summary = "Approve religious organization and issue unique code", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<OrganizationResponseDto> approveOrganization(@PathVariable Long id) {
        OrganizationResponseDto approved = organizationService.approveOrganization(id);
        return ApiResponse.success("Organization approved successfully", approved);
    }

    @PutMapping("/organizations/{id}/reject")
    @Operation(summary = "Reject religious organization registration", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<OrganizationResponseDto> rejectOrganization(
            @PathVariable Long id,
            @RequestBody(required = false) RejectOrganizationRequest request) {
        String reason = request != null ? request.getRejectionReason() : null;
        OrganizationResponseDto rejected = organizationService.rejectOrganization(id, reason);
        return ApiResponse.success("Organization rejected successfully", rejected);
    }
}
