package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.Report;
import com.rememberme.api.repository.ReportRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Report", description = "Report wrong grave/location/details")
public class ReportController {

    private final ReportRepository reportRepository;

    @PostMapping
    public ApiResponse<Report> fileReport(@RequestBody Report report) {
        report.setStatus(Report.ReportStatus.OPEN);
        report.setCreatedAt(LocalDateTime.now());
        return ApiResponse.success("Report submitted", reportRepository.save(report));
    }

    @GetMapping
    public ApiResponse<List<Report>> listReports() {
        return ApiResponse.success(reportRepository.findAll());
    }
}
