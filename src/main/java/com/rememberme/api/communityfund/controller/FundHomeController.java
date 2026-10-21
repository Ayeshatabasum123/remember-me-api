package com.rememberme.api.communityfund.controller;

import com.rememberme.api.communityfund.dto.response.FundHomeResponse;
import com.rememberme.api.communityfund.dto.response.FundImpactResponse;
import com.rememberme.api.communityfund.dto.response.FundPublicBalanceSummaryResponse;
import com.rememberme.api.communityfund.service.FundHomeService;
import com.rememberme.api.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fund")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Community Burial Fund - Home", description = "Public overview, aggregate impact statistics, and approved public balance summaries for the Community Burial Fund")
public class FundHomeController {

    private final FundHomeService fundHomeService;

    @GetMapping("/home")
    @Operation(
            summary = "Get Community Burial Fund Home Information",
            description = "Returns public information about the Community Burial Fund, including fund name, introduction, formal purpose, operational description, donation information, strict privacy guarantees, and funding disclaimer. Accessible to all public users without authentication."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Fund home information retrieved successfully",
                    content = @Content(schema = @Schema(implementation = FundHomeResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<FundHomeResponse>> getFundHome() {
        log.info("Request received for public Community Burial Fund home information");
        FundHomeResponse response = fundHomeService.getFundHomeInfo();
        return ResponseEntity.ok(ApiResponse.success("Fund home information retrieved successfully", response));
    }

    @GetMapping("/impact")
    @Operation(
            summary = "Get Public Impact Statistics",
            description = "Returns approved, aggregated impact statistics: total confirmed donations, total monetary amount received, total completed support cases assisted, and total approved support disbursed. Calculated strictly from confirmed records. Never exposes donor identities, applicant names, or private assistance details."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Impact statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = FundImpactResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<FundImpactResponse>> getFundImpact() {
        log.info("Request received for public Community Burial Fund impact statistics");
        FundImpactResponse response = fundHomeService.getFundImpact();
        return ResponseEntity.ok(ApiResponse.success("Fund impact statistics retrieved successfully", response));
    }

    @GetMapping("/balance-summary")
    @Operation(
            summary = "Get Approved Public Balance Summary",
            description = "Returns approved public balance summary figures (total raised, total disbursed, and net available balance). Strictly protects sensitive financial data: does not expose internal ledger details, pending reserves, or administrative allocations unless authorized."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Public balance summary retrieved successfully",
                    content = @Content(schema = @Schema(implementation = FundPublicBalanceSummaryResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<FundPublicBalanceSummaryResponse>> getFundBalanceSummary() {
        log.info("Request received for public Community Burial Fund balance summary");
        FundPublicBalanceSummaryResponse response = fundHomeService.getPublicBalanceSummary();
        return ResponseEntity.ok(ApiResponse.success("Fund balance summary retrieved successfully", response));
    }
}
