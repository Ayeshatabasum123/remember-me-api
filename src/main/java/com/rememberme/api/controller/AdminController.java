package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.dto.response.GraveImportResponseDto;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.entity.Report;
import com.rememberme.api.repository.RememberMeRepository;
import com.rememberme.api.repository.ReportRepository;
import com.rememberme.api.service.GraveImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'GRAVEYARD_ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Approve/edit/remove rememberMes, duplicate entries, and bulk import graves")
public class AdminController {

    private final RememberMeRepository rememberMeRepository;
    private final ReportRepository reportRepository;
    private final GraveImportService graveImportService;

    @PutMapping("/rememberMes/{id}/approve")
    public ApiResponse<RememberMe> approveGraveyard(@PathVariable Long id) {
        RememberMe rememberMe = rememberMeRepository.findById(id).orElseThrow();
        rememberMe.setStatus(RememberMe.ApprovalStatus.APPROVED);
        return ApiResponse.success("RememberMe approved", rememberMeRepository.save(rememberMe));
    }

    @PutMapping("/rememberMes/{id}/reject")
    public ApiResponse<RememberMe> rejectGraveyard(@PathVariable Long id) {
        RememberMe rememberMe = rememberMeRepository.findById(id).orElseThrow();
        rememberMe.setStatus(RememberMe.ApprovalStatus.REJECTED);
        return ApiResponse.success("RememberMe rejected", rememberMeRepository.save(rememberMe));
    }

    @PutMapping("/reports/{id}/resolve")
    public ApiResponse<Report> resolveReport(@PathVariable Long id) {
        Report report = reportRepository.findById(id).orElseThrow();
        report.setStatus(Report.ReportStatus.RESOLVED);
        return ApiResponse.success("Report resolved", reportRepository.save(report));
    }

    @PostMapping("/notifications/broadcast-vip")
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
    @Operation(
            summary = "Bulk import grave/cemetery records from a CSV file",
            description = "Validates and imports grave records from a CSV file. Supports columns: serialNumber, cemeteryName, graveNumber, latitude, longitude, deceasedName, dateOfBirth (DD/MM/YYYY only), dateOfDeath (DD/MM/YYYY only), biography (required, max 200 chars). Requires Admin JWT authentication and Super Admin password verification.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<GraveImportResponseDto> importGraves(
            @Parameter(description = "CSV file containing grave data", required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Super Admin password for authorization", required = true)
            @RequestParam("superAdminPassword") String superAdminPassword,
            Principal principal) {
        String email = principal != null ? principal.getName() : null;
        GraveImportResponseDto result = graveImportService.importGravesFromCsv(file, superAdminPassword, email);
        return ApiResponse.success("Graves import completed: " + result.getSuccessfulRecords() + " successful, " + result.getFailedRecords() + " failed", result);
    }
}

