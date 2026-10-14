package com.rememberme.api.controller;

import com.rememberme.api.dto.request.AdminRegisterRequest;
import com.rememberme.api.dto.request.CreateEventRequest;
import com.rememberme.api.dto.request.UpdateEventRequest;
import com.rememberme.api.dto.response.*;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.entity.Report;
import com.rememberme.api.repository.RememberMeRepository;
import com.rememberme.api.repository.ReportRepository;
import com.rememberme.api.service.EventService;
import com.rememberme.api.service.GraveImportService;
import com.rememberme.api.service.OrganizationService;
import com.rememberme.api.service.PhotoAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin & Organization registration, event management, and system operations")
public class AdminController {

    private final RememberMeRepository rememberMeRepository;
    private final ReportRepository reportRepository;
    private final GraveImportService graveImportService;
    private final PhotoAuditService photoAuditService;
    private final OrganizationService organizationService;
    private final EventService eventService;

    // --- RELIGIOUS ORGANIZATION ADMIN REGISTRATION (PUBLIC) ---

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("permitAll()")
    @Operation(
            summary = "Register Religious Organization & Admin",
            description = "Allows an Admin to register a religious organization with mandatory photograph upload"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Organization registration details and mandatory photograph file",
            required = true,
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = AdminRegisterRequest.class)
            )
    )
    public ApiResponse<AdminRegisterResponse> registerAdmin(
            @Valid @ModelAttribute AdminRegisterRequest request) {
        MultipartFile file = request.getPhotograph();
        AdminRegisterResponse response = organizationService.registerAdminAndOrganization(request, file);
        return ApiResponse.success("Admin and organization registered successfully. Status: PENDING", response);
    }


    private String getAuthenticatedEmail(Principal principal) {
        if (principal != null && org.springframework.util.StringUtils.hasText(principal.getName())) {
            return principal.getName();
        }
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && org.springframework.util.StringUtils.hasText(auth.getName())) {
            return auth.getName();
        }
        throw new com.rememberme.api.exception.ApiException("Authentication required", org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    // --- ORGANIZATION DETAILS (LOGGED-IN ADMIN) ---

    @GetMapping("/organization/me")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get Logged-in Admin's Organization Details",
            description = "Retrieves the religious organization associated with the currently authenticated admin using their JWT token.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Organization details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AdminOrganizationDetailsResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - requires ADMIN role"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Not Found - organization not found for authenticated admin"
            )
    })
    public ApiResponse<AdminOrganizationDetailsResponse> getMyOrganization(Principal principal) {
        String adminEmail = getAuthenticatedEmail(principal);
        AdminOrganizationDetailsResponse response = organizationService.getAdminOrganizationDetails(adminEmail);
        return ApiResponse.success("Organization details retrieved successfully", response);
    }

    // --- EVENT MANAGEMENT ---

    @PostMapping("/events")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Event", description = "Allows approved organization Admin to create a draft event", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<EventResponseDto> createEvent(@Valid @RequestBody CreateEventRequest request, Principal principal) {
        EventResponseDto created = eventService.createEvent(getAuthenticatedEmail(principal), request);
        return ApiResponse.success("Event created successfully in DRAFT status", created);
    }

    @PutMapping("/events/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Event", description = "Allows approved organization Admin to update an event", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<EventResponseDto> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request,
            Principal principal) {
        EventResponseDto updated = eventService.updateEvent(getAuthenticatedEmail(principal), id, request);
        return ApiResponse.success("Event updated successfully", updated);
    }

    @PostMapping("/events/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Publish Event", description = "Publishes event and triggers postcode-based notifications to registered users", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<EventResponseDto> publishEvent(@PathVariable Long id, Principal principal) {
        EventResponseDto published = eventService.publishEvent(getAuthenticatedEmail(principal), id);
        return ApiResponse.success("Event published successfully and notifications dispatched", published);
    }

    // --- LEGACY ADMIN OPERATIONS ---

    @PutMapping("/rememberMes/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RememberMe> approveGraveyard(@PathVariable Long id) {
        RememberMe rememberMe = rememberMeRepository.findById(id).orElseThrow();
        rememberMe.setStatus(RememberMe.ApprovalStatus.APPROVED);
        return ApiResponse.success("RememberMe approved", rememberMeRepository.save(rememberMe));
    }

    @PutMapping("/rememberMes/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RememberMe> rejectGraveyard(@PathVariable Long id) {
        RememberMe rememberMe = rememberMeRepository.findById(id).orElseThrow();
        rememberMe.setStatus(RememberMe.ApprovalStatus.REJECTED);
        return ApiResponse.success("RememberMe rejected", rememberMeRepository.save(rememberMe));
    }

    @PutMapping("/reports/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Report> resolveReport(@PathVariable Long id) {
        Report report = reportRepository.findById(id).orElseThrow();
        report.setStatus(Report.ReportStatus.RESOLVED);
        return ApiResponse.success("Report resolved", reportRepository.save(report));
    }

    @PostMapping("/notifications/broadcast-vip")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Broadcast a VIP / National Funeral push notification",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Integer> broadcastVipNotification(
            @jakarta.validation.Valid @RequestBody com.rememberme.api.dto.request.VipBroadcastRequest request,
            com.rememberme.api.service.FuneralNotificationEngineService notificationEngineService) {
        int sent = notificationEngineService.broadcastVipFuneral(request);
        return ApiResponse.success("VIP broadcast notification sent to " + sent + " users", sent);
    }

    @PostMapping(value = "/graves/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Bulk import grave/cemetery records from a CSV or Excel file",
            description = "Validates and imports grave records from a CSV (.csv) or Excel (.xls, .xlsx) file. Protected by ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<GraveImportResponseDto> importGraves(
            @Parameter(description = "CSV or Excel file (.csv, .xls, .xlsx) containing grave data", required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Optional Super Admin password (deprecated; JWT role ADMIN is enforced)", required = false)
            @RequestParam(value = "superAdminPassword", required = false) String superAdminPassword,
            Principal principal) {
        String email = principal != null ? principal.getName() : null;
        GraveImportResponseDto result = graveImportService.importGraves(file, email);
        return ApiResponse.success("Graves import completed: " + result.getSuccessfulRecords() + " successful, " + result.getFailedRecords() + " failed", result);
    }

    @GetMapping({"/photos/audit", "/deceased/photo-audit"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Audit existing photo URLs for accessibility",
            description = "Checks existing deceased-person photo URLs against accessibility, size, and format rules. Reports inaccessible URLs without modifying the database.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<PhotoAuditReportDto> auditPhotoUrls() {
        PhotoAuditReportDto report = photoAuditService.auditExistingPhotoUrls();
        return ApiResponse.success("Photo URL audit completed", report);
    }
}
