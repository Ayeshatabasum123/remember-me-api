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
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    public ApiResponse<DeceasedPerson> addDeceasedPerson(@RequestBody DeceasedPersonRequest request) {
        Grave grave = graveRepository.findById(request.getGraveId()).orElseThrow();

        DeceasedPerson person = DeceasedPerson.builder()
                .fullName(request.getFullName())
                .dateOfBirth(request.getDateOfBirth())
                .dateOfDeath(request.getDateOfDeath())
                .gender(request.getGender() != null ? DeceasedPerson.Gender.valueOf(request.getGender()) : null)
                .photoUrl(request.getPhotoUrl())
                .grave(grave)
                .duplicateChecked(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return ApiResponse.success(deceasedPersonRepository.save(person));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeceasedPerson> getDeceasedPerson(@PathVariable Long id) {
        return ApiResponse.success(deceasedPersonRepository.findById(id).orElseThrow());
    }

    @GetMapping
    public ApiResponse<List<DeceasedPerson>> listAll() {
        return ApiResponse.success(deceasedPersonRepository.findAll());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDeceasedPerson(@PathVariable Long id) {
        DeceasedPerson person = deceasedPersonRepository.findById(id)
                .orElseThrow(() -> new ApiException("Deceased person not found with ID: " + id, HttpStatus.NOT_FOUND));

        if (memorialRepository.existsByDeceasedPersonId(id)) {
            throw new ApiException("Cannot delete deceased person: A memorial is linked to this person", HttpStatus.BAD_REQUEST);
        }

        if (relationshipRepository.existsByDeceasedPersonId(id)) {
            throw new ApiException("Cannot delete deceased person: Active relationships are linked to this person", HttpStatus.BAD_REQUEST);
        }

        deceasedPersonRepository.delete(person);
        return ApiResponse.success("Deceased person record deleted successfully", null);
    }
}
