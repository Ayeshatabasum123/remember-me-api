package com.rememberme.api.communityfund.dto.response;

import com.rememberme.api.communityfund.enums.SupportDocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of an uploaded supporting document")
public class SupportDocumentResponseDto {

    @Schema(description = "Document ID", example = "10")
    private Long id;

    @Schema(description = "Associated support request ID", example = "1")
    private Long supportRequestId;

    @Schema(description = "Type of document: INVOICE, QUOTATION, SUPPORTING_DOCUMENT, etc.", example = "INVOICE")
    private SupportDocumentType documentType;

    @Schema(description = "Original filename", example = "burial_invoice.pdf")
    private String originalFilename;

    @Schema(description = "MIME Content-Type", example = "application/pdf")
    private String contentType;

    @Schema(description = "File size in bytes", example = "1048576")
    private Long fileSize;

    @Schema(description = "ID of user who uploaded document", example = "5")
    private Long uploadedByUserId;

    @Schema(description = "Email of user who uploaded document", example = "applicant@example.com")
    private String uploadedByEmail;

    @Schema(description = "Upload timestamp", example = "2026-10-22T12:00:00")
    private LocalDateTime uploadedAt;
}
