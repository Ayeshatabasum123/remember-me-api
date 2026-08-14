package com.rememberme.api.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DeceasedPersonRequest {
    private String fullName;
    private LocalDate dateOfBirth;
    private LocalDate dateOfDeath;
    private String gender;
    private String photoUrl;
    private Long graveId;
}
