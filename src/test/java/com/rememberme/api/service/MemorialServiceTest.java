package com.rememberme.api.service;

import com.rememberme.api.dto.response.MemorialSummaryDto;
import com.rememberme.api.dto.response.RecentMemorialsResponseDto;
import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.Memorial;
import com.rememberme.api.repository.MemorialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.rememberme.api.exception.ApiException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MemorialServiceTest {

    @Mock
    private MemorialRepository memorialRepository;

    @InjectMocks
    private MemorialService memorialService;

    private Memorial memorial1;
    private Memorial memorial2;

    @BeforeEach
    public void setUp() {
        DeceasedPerson deceased1 = DeceasedPerson.builder()
                .id(100L)
                .fullName("Eleanor Vance")
                .photoUrl("https://s3.amazonaws.com/bucket/eleanor.jpg")
                .dateOfBirth(LocalDate.of(1945, 3, 15))
                .dateOfDeath(LocalDate.of(2023, 8, 20))
                .build();

        memorial1 = Memorial.builder()
                .id(1L)
                .deceasedPerson(deceased1)
                .biography("A wonderful soul who lit up every room.")
                .prayerOrMessage("Rest in peace.")
                .createdAt(LocalDateTime.of(2026, 8, 30, 10, 0))
                .build();

        DeceasedPerson deceased2 = DeceasedPerson.builder()
                .id(101L)
                .fullName("Arthur Pendelton")
                .photoUrl("https://s3.amazonaws.com/bucket/arthur.jpg")
                .dateOfBirth(LocalDate.of(1930, 11, 5))
                .dateOfDeath(LocalDate.of(2022, 1, 10))
                .build();

        memorial2 = Memorial.builder()
                .id(2L)
                .deceasedPerson(deceased2)
                .biography("") // Empty biography to test prayerOrMessage fallback
                .prayerOrMessage("In loving memory of Arthur.")
                .createdAt(LocalDateTime.of(2026, 8, 29, 14, 30))
                .build();
    }

    @Test
    public void deleteMemorial_Success() {
        when(memorialRepository.findById(1L)).thenReturn(Optional.of(memorial1));

        memorialService.deleteMemorial(1L);

        verify(memorialRepository).delete(memorial1);
    }

    @Test
    public void deleteMemorial_NotFound() {
        when(memorialRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> memorialService.deleteMemorial(999L));
        assertEquals("Memorial not found with ID: 999", exception.getMessage());
    }

    @Test
    public void getRecentMemorialSummaries_MapsEntitiesCorrectly() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Memorial> page = new PageImpl<>(List.of(memorial1, memorial2), pageable, 2);

        when(memorialRepository.findAll(pageable)).thenReturn(page);

        RecentMemorialsResponseDto response = memorialService.getRecentMemorialSummaries(pageable);

        assertNotNull(response);
        assertEquals(2, response.getMemorials().size());

        RecentMemorialsResponseDto.PaginationDto pagination = response.getPagination();
        assertEquals(0, pagination.getCurrentPage());
        assertEquals(10, pagination.getPageSize());
        assertEquals(2, pagination.getTotalRecords());
        assertEquals(1, pagination.getTotalPages());
        assertFalse(pagination.isHasNext());

        MemorialSummaryDto dto1 = response.getMemorials().get(0);
        assertEquals(1L, dto1.getMemorialId());
        assertEquals("Eleanor Vance", dto1.getDeceasedName());
        assertEquals("https://s3.amazonaws.com/bucket/eleanor.jpg", dto1.getPhotoUrl());
        assertEquals(LocalDate.of(1945, 3, 15), dto1.getDateOfBirth());
        assertEquals(LocalDate.of(2023, 8, 20), dto1.getDateOfDeath());
        assertEquals("A wonderful soul who lit up every room.", dto1.getBiography());

        // Test fallback to prayerOrMessage when biography is empty
        MemorialSummaryDto dto2 = response.getMemorials().get(1);
        assertEquals(2L, dto2.getMemorialId());
        assertEquals("Arthur Pendelton", dto2.getDeceasedName());
        assertEquals("In loving memory of Arthur.", dto2.getBiography());
    }
}
