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
    @Schema(example = "St. Paul's Church")
    private String organizationName;

    @NotNull(message = "Organization type is required (CHURCH, MOSQUE, TEMPLE, OTHER)")
    @Schema(example = "CHURCH")
    private OrganizationType organizationType;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(example = "admin@stpauls.org")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Schema(example = "+442079460000")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long.")
    @Schema(example = "AdminPass123!")
    private String password;

    @NotBlank(message = "Confirm password is required")
    @Schema(example = "AdminPass123!")
    private String confirmPassword;

    @NotBlank(message = "Organization postcode is required")
    @Schema(example = "SW1A 1AA")
    private String postcode;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @Schema(example = "51.5014")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @Schema(example = "-0.1419")
    private Double longitude;

    // Optional URL if not using multipart upload
    @Schema(example = "https://example.com/church.jpg")
    private String photoUrl;

    // Optional direct multipart file mapping for multipart/form-data
    private MultipartFile photograph;
}
