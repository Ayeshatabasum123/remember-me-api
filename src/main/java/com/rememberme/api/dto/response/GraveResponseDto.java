package com.rememberme.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.Grave;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GraveResponseDto {
    private Long id;
    private String city;
    private String country;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Grave or plot number returned as String (e.g. '1', '12', 'A101', 'GR-205', 'Unknown')", example = "A101")
    private String graveNumber;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Postcode returned as String (e.g. 'SW1A 1AA', 'NW3 2QD', 'EC4M 8AD')", example = "SW1A 1AA")
    private String postcode;

    private Double latitude;
    private Double longitude;
    private Double locationAccuracy;
    private Grave.VerificationStatus verificationStatus;

    // Graveyard / Cemetery details
    private Long graveyardId;
    private Long rememberMeId;
    private String graveyardName;
    private String cemeteryName;

    // Deceased person details
    private Long deceasedPersonId;
    private String deceasedName;
    private LocalDate dateOfBirth;
    private LocalDate dateOfDeath;
    private String biography;
    private DeceasedPerson.Gender gender;
    private String photoUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static String parseGraveNumber(String graveNumber) {
        return graveNumber != null ? graveNumber.trim() : null;
    }
}
