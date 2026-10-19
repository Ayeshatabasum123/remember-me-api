package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.dto.response.FundSummaryResponse;
import com.rememberme.api.service.FundLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fund")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Community Burial Fund", description = "Public aggregate statistics and balance summary for the shared Community Burial Fund")
public class FundController {

    private final FundLedgerService fundLedgerService;

    @GetMapping("/summary")
    @Operation(summary = "Get Community Burial Fund Summary", description = "Returns approved aggregate financial figures only: total settled donations, gross raised, fees paid, refunds, and current available net balance. Never exposes donor identities, dedications, or private support details.")
    public ApiResponse<FundSummaryResponse> getFundSummary() {
        FundSummaryResponse summary = fundLedgerService.getFundSummary();
        return ApiResponse.success("Fund summary retrieved successfully", summary);
    }
}
