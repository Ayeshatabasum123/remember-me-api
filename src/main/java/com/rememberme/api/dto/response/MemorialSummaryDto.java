package com.rememberme.api.dto.response;

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
public class MemorialSummaryDto {
    private Long memorialId;
    private String deceasedName;
    private String photoUrl;
    private LocalDate dateOfBirth;
    private LocalDate dateOfDeath;
    private String biography;
    private LocalDateTime createdAt;
}
