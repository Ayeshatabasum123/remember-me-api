package com.rememberme.api.controller;

import com.rememberme.api.dto.request.DeceasedPersonRequest;
import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.Grave;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.DeceasedPersonRepository;
import com.rememberme.api.repository.GraveRepository;
import com.rememberme.api.repository.MemorialRepository;
import com.rememberme.api.repository.RelationshipRepository;
import com.rememberme.api.service.PhotoUrlValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/deceased")
@RequiredArgsConstructor
@Tag(name = "Deceased Person", description = "Stores information about the deceased")
public class DeceasedPersonController {

    private final DeceasedPersonRepository deceasedPersonRepository;
    private final GraveRepository graveRepository;
    private final MemorialRepository memorialRepository;
    private final RelationshipRepository relationshipRepository;
    private final PhotoUrlValidator photoUrlValidator;

    @PostMapping
    @Operation(summary = "Add a deceased person record with validated photo URL")
    public ApiResponse<DeceasedPerson> addDeceasedPerson(@RequestBody DeceasedPersonRequest request) {
        Grave grave = graveRepository.findById(request.getGraveId())
                .orElseThrow(() -> new ApiException("Grave not found with ID: " + request.getGraveId(), HttpStatus.NOT_FOUND));

        String photoUrl = null;
        if (request.getPhotoUrl() != null && !request.getPhotoUrl().trim().isEmpty()) {
            PhotoUrlValidator.ValidationResult result = photoUrlValidator.validatePhotoUrl(request.getPhotoUrl().trim());
            if (!result.isValid()) {
                throw new ApiException(result.getErrorMessage(), HttpStatus.BAD_REQUEST);
            }
            photoUrl = (result.getResolvedUrl() != null && !result.getResolvedUrl().trim().isEmpty())
                    ? result.getResolvedUrl().trim()
                    : request.getPhotoUrl().trim();
        }

        DeceasedPerson person = DeceasedPerson.builder()
                .fullName(request.getFullName())
                .dateOfBirth(request.getDateOfBirth())
                .dateOfDeath(request.getDateOfDeath())
                .gender(request.getGender() != null ? DeceasedPerson.Gender.valueOf(request.getGender()) : null)
                .photoUrl(photoUrl)
                .grave(grave)
                .duplicateChecked(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return ApiResponse.success(deceasedPersonRepository.save(person));
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Update a deceased person record with validated photo URL")
    public ApiResponse<DeceasedPerson> updateDeceasedPerson(
            @PathVariable Long id,
            @RequestBody DeceasedPersonRequest request) {
        DeceasedPerson person = deceasedPersonRepository.findById(id)
                .orElseThrow(() -> new ApiException("Deceased person not found with ID: " + id, HttpStatus.NOT_FOUND));

        if (request.getFullName() != null) {
            person.setFullName(request.getFullName());
        }
        if (request.getDateOfBirth() != null) {
            person.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getDateOfDeath() != null) {
            person.setDateOfDeath(request.getDateOfDeath());
        }
        if (request.getGender() != null) {
            person.setGender(DeceasedPerson.Gender.valueOf(request.getGender()));
        }
        if (request.getGraveId() != null) {
            Grave grave = graveRepository.findById(request.getGraveId())
                    .orElseThrow(() -> new ApiException("Grave not found with ID: " + request.getGraveId(), HttpStatus.NOT_FOUND));
            person.setGrave(grave);
        }

        if (request.getPhotoUrl() != null) {
            if (!request.getPhotoUrl().trim().isEmpty()) {
                PhotoUrlValidator.ValidationResult result = photoUrlValidator.validatePhotoUrl(request.getPhotoUrl().trim());
                if (!result.isValid()) {
                    throw new ApiException(result.getErrorMessage(), HttpStatus.BAD_REQUEST);
                }
                String effectiveUrl = (result.getResolvedUrl() != null && !result.getResolvedUrl().trim().isEmpty())
                        ? result.getResolvedUrl().trim()
                        : request.getPhotoUrl().trim();
                person.setPhotoUrl(effectiveUrl);
            } else {
                person.setPhotoUrl(null);
            }
        }

        person.setUpdatedAt(LocalDateTime.now());
        return ApiResponse.success("Deceased person record updated successfully", deceasedPersonRepository.save(person));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeceasedPerson> getDeceasedPerson(@PathVariable Long id) {
        return ApiResponse.success(deceasedPersonRepository.findById(id)
                .orElseThrow(() -> new ApiException("Deceased person not found with ID: " + id, HttpStatus.NOT_FOUND)));
    }

    @GetMapping
    public ApiResponse<List<DeceasedPerson>> listAll() {
        return ApiResponse.success(deceasedPersonRepository.findAll());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ApiResponse<Void> deleteDeceasedPerson(@PathVariable Long id) {
        DeceasedPerson person = deceasedPersonRepository.findById(id)
                .orElseThrow(() -> new ApiException("Deceased person not found with ID: " + id, HttpStatus.NOT_FOUND));

        memorialRepository.deleteByDeceasedPersonId(id);
        relationshipRepository.deleteByDeceasedPersonId(id);

        deceasedPersonRepository.delete(person);
        return ApiResponse.success("Deceased person record deleted successfully", null);
    }
}
