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
    private Object graveNumber;
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

    public static Object parseGraveNumber(String graveNumber) {
        if (graveNumber == null) {
            return null;
        }
        String trimmed = graveNumber.trim();
        if (trimmed.matches("^-?\\d+$")) {
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException ignored) {
            }
        }
        return trimmed;
    }
}
