package com.rememberme.api.communityfund.controller;

import com.rememberme.api.communityfund.dto.request.CreateSupportRequestDto;
import com.rememberme.api.communityfund.dto.request.UpdateSupportRequestDto;
import com.rememberme.api.communityfund.dto.request.SubmitAdditionalInfoDto;
import com.rememberme.api.communityfund.dto.response.*;
import com.rememberme.api.communityfund.enums.SupportDocumentType;
import com.rememberme.api.communityfund.security.CommunityFundSecurityHelper;
import com.rememberme.api.communityfund.service.SupportRequestService;
import com.rememberme.api.communityfund.service.SupportTrackingService;
import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/support-requests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Community Burial Fund - Support Requests", description = "Endpoints for private burial financial support requests, document uploads, and confidential applicant access")
@SecurityRequirement(name = "bearerAuth")
public class SupportRequestController {

    private final SupportRequestService supportRequestService;
    private final SupportTrackingService supportTrackingService;
    private final CommunityFundSecurityHelper securityHelper;

    @PostMapping
    @Operation(
            summary = "Create Support Request",
            description = "Privately request financial assistance for burial expenses. Collects applicant details, burial provider contact, financial costs, and urgency. Authenticated user is automatically assigned as applicant."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Support request submitted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed for costs, currency, or missing required fields"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Idempotency conflict")
    })
    public ResponseEntity<ApiResponse<SupportRequestCreateResponseDto>> createSupportRequest(
            @Valid @RequestBody CreateSupportRequestDto requestDto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        User applicant = securityHelper.getCurrentAuthenticatedUser();
        log.info("Received support request creation from user ID {} ({})", applicant.getId(), applicant.getEmail());
        SupportRequestCreateResponseDto response = supportRequestService.createSupportRequest(applicant, requestDto, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success("Support request submitted successfully", response));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get My Applications",
            description = "Confidential endpoint to retrieve financial support applications submitted by the logged-in applicant. Supports pagination."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Applications retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<ApiResponse<Page<SupportRequestSummaryDto>>> getMyApplications(
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort property", example = "createdAt") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction", example = "DESC") @RequestParam(defaultValue = "DESC") String direction) {
        User applicant = securityHelper.getCurrentAuthenticatedUser();
        Page<SupportRequestSummaryDto> response = supportTrackingService.getMyApplications(applicant, page, size, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.success("Applications retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get Support Request Details",
            description = "Confidential endpoint to fetch application details. Restricted strictly to the applicant owner or authorized administrators."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Support request details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not your application"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Support request not found")
    })
    public ResponseEntity<ApiResponse<SupportRequestResponseDto>> getSupportRequest(
            @Parameter(description = "Support request ID", example = "1") @PathVariable Long id) {
        User currentUser = securityHelper.getCurrentAuthenticatedUser();
        SupportRequestResponseDto response = supportRequestService.getSupportRequest(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Support request retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update Support Request",
            description = "Update details of an editable support request. Permitted only when status is PENDING or MORE_INFO_REQUIRED."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Support request updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Request is in non-editable status or validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not your application"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Support request not found")
    })
    public ResponseEntity<ApiResponse<SupportRequestResponseDto>> updateSupportRequest(
            @Parameter(description = "Support request ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateSupportRequestDto updateDto) {
        User currentUser = securityHelper.getCurrentAuthenticatedUser();
        SupportRequestResponseDto response = supportRequestService.updateSupportRequest(id, updateDto, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Support request updated successfully", response));
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload Supporting Document",
            description = "Upload a private supporting document (PDF, JPG, JPEG, PNG, max 5 MB). Validates document type (INVOICE, QUOTATION, SUPPORTING_DOCUMENT) and prevents duplicate invoices."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Supporting document uploaded successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid file type, file exceeds 5MB, or duplicate invoice detected"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not your application"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Support request not found")
    })
    public ResponseEntity<ApiResponse<SupportDocumentResponseDto>> uploadDocument(
            @Parameter(description = "Support request ID", example = "1") @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentType", required = false, defaultValue = "SUPPORTING_DOCUMENT") SupportDocumentType documentType) {
        User currentUser = securityHelper.getCurrentAuthenticatedUser();
        log.info("User ID {} uploading document (type: {}) for support request ID {}", currentUser.getId(), documentType, id);
        SupportDocumentResponseDto response = supportRequestService.uploadDocument(id, file, documentType, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", response));
    }

    @GetMapping("/{id}/documents")
    @Operation(
            summary = "List Uploaded Documents",
            description = "Returns metadata list of all private documents uploaded for this application. Restricted to applicant owner or administrators."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Documents retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Support request not found")
    })
    public ResponseEntity<ApiResponse<List<SupportDocumentResponseDto>>> listDocuments(
            @Parameter(description = "Support request ID", example = "1") @PathVariable Long id) {
        User currentUser = securityHelper.getCurrentAuthenticatedUser();
        List<SupportDocumentResponseDto> documents = supportRequestService.listDocuments(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", documents));
    }

    @GetMapping("/{id}/documents/{documentId}")
    @Operation(
            summary = "Securely Download Document",
            description = "Secure streaming download for private document files. Never exposed through public URLs. Restricted to applicant owner or administrators."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Document streamed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Document or request not found")
    })
    public ResponseEntity<Resource> downloadDocument(
            @Parameter(description = "Support request ID", example = "1") @PathVariable Long id,
            @Parameter(description = "Document ID", example = "10") @PathVariable Long documentId) {
        User currentUser = securityHelper.getCurrentAuthenticatedUser();
        SupportRequestService.DownloadedDocument downloaded = supportRequestService.downloadDocument(id, documentId, currentUser);

        String disposition = String.format("attachment; filename=\"%s\"", downloaded.originalFilename().replace("\"", ""));
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(downloaded.contentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(downloaded.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(downloaded.resource());
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @Operation(
            summary = "Remove Supporting Document",
            description = "Delete an uploaded document when permitted. Permitted only in editable states (PENDING, MORE_INFO_REQUIRED) for the applicant, or by administrators."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Document removed successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Application is in non-editable status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Document or request not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @Parameter(description = "Support request ID", example = "1") @PathVariable Long id,
            @Parameter(description = "Document ID", example = "10") @PathVariable Long documentId) {
        User currentUser = securityHelper.getCurrentAuthenticatedUser();
        supportRequestService.deleteDocument(id, documentId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully", null));
    }

    @GetMapping("/{id}/status")
    @Operation(
            summary = "Get Support Request Status",
            description = "Retrieve the current progress and contextual status message of an application."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Support request not found")
    })
    public ResponseEntity<ApiResponse<SupportRequestStatusResponseDto>> getRequestStatus(
            @Parameter(description = "Support request ID", example = "1") @PathVariable Long id) {
        User currentUser = securityHelper.getCurrentAuthenticatedUser();
        SupportRequestStatusResponseDto response = supportTrackingService.getRequestStatus(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Status retrieved successfully", response));
    }

    @GetMapping("/{id}/updates")
    @Operation(
            summary = "View Application Updates",
            description = "View confidential chronological updates for a financial support application."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Updates retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Support request not found")
    })
    public ResponseEntity<ApiResponse<List<SupportRequestUpdateResponseDto>>> getRequestUpdates(
            @Parameter(description = "Support request ID", example = "1") @PathVariable Long id) {
        User currentUser = securityHelper.getCurrentAuthenticatedUser();
        List<SupportRequestUpdateResponseDto> response = supportTrackingService.getRequestUpdates(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Updates retrieved successfully", response));
    }

    @PostMapping("/{id}/additional-info")
    @Operation(
            summary = "Submit Additional Information",
            description = "Submit requested explanation and link uploaded supporting documents in response to an administrative request for more info. Transitions status from MORE_INFO_REQUIRED back to UNDER_REVIEW."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Additional information submitted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or document ID does not belong to request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Support request not found")
    })
    public ResponseEntity<ApiResponse<SupportRequestResponseDto>> submitAdditionalInfo(
            @Parameter(description = "Support request ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody SubmitAdditionalInfoDto dto) {
        User currentUser = securityHelper.getCurrentAuthenticatedUser();
        SupportRequestResponseDto response = supportTrackingService.submitAdditionalInfo(id, dto, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Additional information submitted successfully", response));
    }

    @GetMapping("/{id}/history")
    @Operation(
            summary = "View Authorized Status History",
            description = "View paginated audit history of status transitions for this application."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "History retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Support request not found")
    })
    public ResponseEntity<ApiResponse<Page<SupportRequestUpdateResponseDto>>> getRequestHistory(
            @Parameter(description = "Support request ID", example = "1") @PathVariable Long id,
            @Parameter(description = "Page number", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size) {
        User currentUser = securityHelper.getCurrentAuthenticatedUser();
        Page<SupportRequestUpdateResponseDto> response = supportTrackingService.getRequestHistory(id, currentUser, page, size);
        return ResponseEntity.ok(ApiResponse.success("Status history retrieved successfully", response));
    }
}
