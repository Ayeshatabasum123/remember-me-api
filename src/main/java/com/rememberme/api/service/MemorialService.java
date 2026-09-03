package com.rememberme.api.service;

import com.rememberme.api.dto.response.MemorialSummaryDto;
import com.rememberme.api.dto.response.PaginatedResponse;
import com.rememberme.api.dto.response.RecentMemorialDto;
import com.rememberme.api.dto.response.RecentMemorialsResponseDto;
import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.Memorial;
import com.rememberme.api.repository.MemorialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.rememberme.api.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemorialService {

    private final MemorialRepository memorialRepository;

    public void deleteMemorial(Long id) {
        Memorial memorial = memorialRepository.findById(id)
                .orElseThrow(() -> new ApiException("Memorial not found with ID: " + id, HttpStatus.NOT_FOUND));
        memorialRepository.delete(memorial);
    }

    public RecentMemorialsResponseDto getRecentMemorialSummaries(Pageable pageable) {
        Page<Memorial> page = memorialRepository.findAll(pageable);

        List<MemorialSummaryDto> summaries = page.getContent().stream()
                .map(this::mapToMemorialSummaryDto)
                .collect(Collectors.toList());

        RecentMemorialsResponseDto.PaginationDto pagination = RecentMemorialsResponseDto.PaginationDto.builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalRecords(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();

        return RecentMemorialsResponseDto.builder()
                .memorials(summaries)
                .pagination(pagination)
                .build();
    }

    private MemorialSummaryDto mapToMemorialSummaryDto(Memorial memorial) {
        DeceasedPerson deceased = memorial.getDeceasedPerson();

        String bioText = memorial.getBiography();
        if (bioText == null || bioText.trim().isEmpty()) {
            bioText = memorial.getPrayerOrMessage();
        }

        return MemorialSummaryDto.builder()
                .memorialId(memorial.getId())
                .deceasedName(deceased != null ? deceased.getFullName() : null)
                .photoUrl(deceased != null ? deceased.getPhotoUrl() : null)
                .dateOfBirth(deceased != null ? deceased.getDateOfBirth() : null)
                .dateOfDeath(deceased != null ? deceased.getDateOfDeath() : null)
                .biography(bioText)
                .createdAt(memorial.getCreatedAt())
                .build();
    }
}

