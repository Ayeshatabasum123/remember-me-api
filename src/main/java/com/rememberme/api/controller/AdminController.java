package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.entity.Report;
import com.rememberme.api.repository.RememberMeRepository;
import com.rememberme.api.repository.ReportRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'GRAVEYARD_ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Approve/edit/remove rememberMes and duplicate entries")
public class AdminController {

    private final RememberMeRepository rememberMeRepository;
    private final ReportRepository reportRepository;

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
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Broadcast a VIP / National Funeral push notification",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Integer> broadcastVipNotification(
            @jakarta.validation.Valid @RequestBody com.rememberme.api.dto.request.VipBroadcastRequest request,
            com.rememberme.api.service.FuneralNotificationEngineService notificationEngineService) {
        int sent = notificationEngineService.broadcastVipFuneral(request);
        return ApiResponse.success("VIP broadcast notification sent to " + sent + " users", sent);
    }
}
