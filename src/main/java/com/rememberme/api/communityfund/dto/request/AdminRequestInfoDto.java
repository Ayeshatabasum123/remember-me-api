package com.rememberme.api.communityfund.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin request for additional applicant information")
public class AdminRequestInfoDto {

    @NotBlank(message = "Message describing required information is required")
    @Schema(description = "Confidential message to the applicant explaining what information or documents are required", example = "Please upload an updated burial invoice.")
    private String message;

    @Schema(description = "Optional list of document types required (e.g. INVOICE, QUOTATION)", example = "[\"INVOICE\"]")
    private List<String> requiredDocumentTypes;
}
