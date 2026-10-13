package com.rememberme.api.dto.request;

import com.rememberme.api.entity.OrganizationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRegisterRequest {

    @NotBlank(message = "Organization name is required")
    @Schema(description = "Organization name", example = "St. Paul's Church", requiredMode = Schema.RequiredMode.REQUIRED)
    private String organizationName;

    @NotNull(message = "Organization type is required (CHURCH, MOSQUE, TEMPLE, OTHER)")
    @Schema(description = "Organization type", example = "CHURCH", requiredMode = Schema.RequiredMode.REQUIRED)
    private OrganizationType organizationType;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "Admin contact email address", example = "admin@stpauls.org", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Schema(description = "Contact phone number", example = "+442079460000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long.")
    @Schema(description = "Admin password (minimum 8 characters)", example = "AdminPass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "Confirm password is required")
    @Schema(description = "Confirm password (must match password)", example = "AdminPass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String confirmPassword;

    @NotBlank(message = "Organization postcode is required")
    @Schema(description = "Valid UK postcode", example = "SW1A 1AA", requiredMode = Schema.RequiredMode.REQUIRED)
    private String postcode;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @Schema(description = "Organization latitude (-90 to 90)", example = "51.5014", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @Schema(description = "Organization longitude (-180 to 180)", example = "-0.1419", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double longitude;

    @NotNull(message = "Organization photograph is required")
    @Schema(
            description = "Organization photograph image file (JPG, JPEG, or PNG format, max 5MB)",
            type = "string",
            format = "binary",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private MultipartFile photograph;
}

