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
import java.util.List;

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

    @GetMapping("/events")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get Organization Events",
            description = "Retrieves all events (draft and published) belonging to the authenticated Admin's organization",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Organization events retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - requires ADMIN role"
            )
    })
    public ApiResponse<List<EventResponseDto>> getAdminEvents(Principal principal) {
        String adminEmail = getAuthenticatedEmail(principal);
        List<EventResponseDto> events = eventService.getAdminEvents(adminEmail);
        return ApiResponse.success("Organization events retrieved successfully", events);
    }

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
            summary = "Bulk import grave and deceased records from CSV or Excel file",
            description = "Validates and imports grave records from a CSV (.csv) or Excel (.xls, .xlsx) file. Protected by ADMIN role.\n\n" +
                    "### Supported File Types\n" +
                    "- `.csv` (Comma Separated Values, UTF-8, RFC 4180 compliant)\n" +
                    "- `.xls` (Microsoft Excel 97-2003 format)\n" +
                    "- `.xlsx` (Microsoft Excel OpenXML format)\n\n" +
                    "### Required & Optional Column Specifications (13 Columns)\n" +
                    "Columns may appear in any order in the file header:\n\n" +
                    "| # | Column Name | Data Type | Requirement & Validation |\n" +
                    "|---|---|---|---|\n" +
                    "| 1 | `serialNumber` | String | **Required**. Sequential row/record identifier (e.g. '1', '2'). |\n" +
                    "| 2 | `cemeteryName` | String | **Required**. Name of graveyard/cemetery (auto-created if new). |\n" +
                    "| 3 | `city` | String | **Required**. City of the cemetery (maximum 100 characters). |\n" +
                    "| 4 | `country` | String | **Required**. Country of the cemetery (maximum 100 characters). |\n" +
                    "| 5 | `graveNumber` | String | **Required**. Alphanumeric or numeric string plot number (e.g., '1', '12', 'A101', 'GR-205', 'Unknown'). Supports 'Unknown' for unidentified plots. |\n" +
                    "| 6 | `postcode` | String | **Optional** (nullable). Postal code stored as String (e.g., 'SW1A 1AA', 'NW3 2QD', 'EC4M 8AD', '01234'), preserving leading zeros and spaces (max 20 chars). |\n" +
                    "| 7 | `latitude` | Double | **Required**. Decimal latitude between -90.0 and 90.0. |\n" +
                    "| 8 | `longitude` | Double | **Required**. Decimal longitude between -180.0 and 180.0. |\n" +
                    "| 9 | `deceasedName` | String | **Required**. Full name of the deceased person (maximum 100 characters). |\n" +
                    "| 10 | `dateOfBirth` | LocalDate | **Required**. Birth date in `DD/MM/YYYY` or `DD-MM-YYYY` format (or Excel native date cell). |\n" +
                    "| 11 | `dateOfDeath` | LocalDate | **Required**. Death date in `DD/MM/YYYY` or `DD-MM-YYYY` format (must be on or after dateOfBirth). |\n" +
                    "| 12 | `biography` | String | **Required**. Biographical summary (maximum 200 characters). |\n" +
                    "| 13 | `photoUrl` | String | **Optional** (nullable). Valid HTTPS JPEG image URL (maximum file size 1 MB). Rejected if PNG, GIF, WebP, or oversized. |\n\n" +
                    "### Example Success Response (200 OK)\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"success\": true,\n" +
                    "  \"message\": \"Graves import completed: 20 successful, 0 failed\",\n" +
                    "  \"data\": {\n" +
                    "    \"totalRecords\": 20,\n" +
                    "    \"successfulRecords\": 20,\n" +
                    "    \"failedRecords\": 0,\n" +
                    "    \"errors\": []\n" +
                    "  }\n" +
                    "}\n" +
                    "```\n\n" +
                    "### Example Validation Error Response (200 OK with Row Errors)\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"success\": true,\n" +
                    "  \"message\": \"Graves import completed: 18 successful, 2 failed\",\n" +
                    "  \"data\": {\n" +
                    "    \"totalRecords\": 20,\n" +
                    "    \"successfulRecords\": 18,\n" +
                    "    \"failedRecords\": 2,\n" +
                    "    \"errors\": [\n" +
                    "      {\n" +
                    "        \"row\": 9,\n" +
                    "        \"message\": \"Only JPG and JPEG image URLs are allowed.\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"row\": 17,\n" +
                    "        \"message\": \"Image size must not exceed 1 MB.\"\n" +
                    "      }\n" +
                    "    ]\n" +
                    "  }\n" +
                    "}\n" +
                    "```",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Import process completed. Contains counts of successful/failed records and detailed row-level error messages.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GraveImportResponseDto.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid file format, empty file, or missing required column headers."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - requires ADMIN role"
            )
    })
    public ApiResponse<GraveImportResponseDto> importGraves(
            @Parameter(description = "Upload CSV (.csv) or Excel (.xls, .xlsx) file containing grave records with columns: serialNumber, cemeteryName, city, country, graveNumber, postcode, latitude, longitude, deceasedName, dateOfBirth, dateOfDeath, biography, photoUrl", required = true,
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
