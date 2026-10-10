package com.rememberme.api.service;

import com.rememberme.api.dto.response.PhotoAuditReportDto;
import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.Grave;
import com.rememberme.api.repository.DeceasedPersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PhotoAuditServiceTest {

    @Mock
    private DeceasedPersonRepository deceasedPersonRepository;

    @Mock
    private PhotoUrlValidator photoUrlValidator;

    @InjectMocks
    private PhotoAuditService photoAuditService;

    @Test
    public void auditExistingPhotoUrls_FindsAccessibleAndInaccessible_DoesNotModifyDatabase() {
        Grave grave1 = Grave.builder().id(101L).build();
        Grave grave2 = Grave.builder().id(102L).build();

        DeceasedPerson person1 = DeceasedPerson.builder()
                .id(1L)
                .fullName("Valid Person")
                .photoUrl("https://upload.wikimedia.org/wikipedia/commons/6/6f/Valid.jpg")
                .grave(grave1)
                .build();

        DeceasedPerson person2 = DeceasedPerson.builder()
                .id(2L)
                .fullName("Invalid Person")
                .photoUrl("https://example.com/broken.jpg")
                .grave(grave2)
                .build();

        DeceasedPerson personNoPhoto = DeceasedPerson.builder()
                .id(3L)
                .fullName("No Photo Person")
                .photoUrl(null)
                .build();

        when(deceasedPersonRepository.findAll()).thenReturn(List.of(person1, person2, personNoPhoto));
        when(photoUrlValidator.validatePhotoUrl(person1.getPhotoUrl()))
                .thenReturn(PhotoUrlValidator.ValidationResult.success("https://upload.wikimedia.org/wikipedia/commons/6/6f/Valid.jpg"));
        when(photoUrlValidator.validatePhotoUrl(person2.getPhotoUrl()))
                .thenReturn(PhotoUrlValidator.ValidationResult.failure("Image URL is not accessible."));

        PhotoAuditReportDto report = photoAuditService.auditExistingPhotoUrls();

        assertNotNull(report);
        assertEquals(2, report.getTotalChecked());
        assertEquals(1, report.getAccessibleCount());
        assertEquals(1, report.getInaccessibleCount());
        assertEquals(1, report.getInaccessibleRecords().size());

        assertEquals(2L, report.getInaccessibleRecords().get(0).getDeceasedPersonId());
        assertEquals("Invalid Person", report.getInaccessibleRecords().get(0).getDeceasedName());
        assertEquals(102L, report.getInaccessibleRecords().get(0).getGraveId());
        assertEquals("https://example.com/broken.jpg", report.getInaccessibleRecords().get(0).getPhotoUrl());
        assertEquals("Image URL is not accessible.", report.getInaccessibleRecords().get(0).getReason());

        // Verify that NO database writes or saves occurred
        verify(deceasedPersonRepository, never()).save(any());
        verify(deceasedPersonRepository, never()).saveAll(any());
        verify(deceasedPersonRepository, never()).delete(any());
    }
}
