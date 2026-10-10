package com.rememberme.api.service;

import com.rememberme.api.dto.response.InaccessiblePhotoRecordDto;
import com.rememberme.api.dto.response.PhotoAuditReportDto;
import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.repository.DeceasedPersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoAuditService {

    private final DeceasedPersonRepository deceasedPersonRepository;
    private final PhotoUrlValidator photoUrlValidator;

    /**
     * Scans existing deceased person records for inaccessible photo URLs.
     * Reports findings WITHOUT automatically modifying or overwriting any database records.
     */
    @Transactional(readOnly = true)
    public PhotoAuditReportDto auditExistingPhotoUrls() {
        List<DeceasedPerson> persons = deceasedPersonRepository.findAll();
        int totalChecked = 0;
        int accessibleCount = 0;
        int inaccessibleCount = 0;
        List<InaccessiblePhotoRecordDto> inaccessibleList = new ArrayList<>();

        for (DeceasedPerson person : persons) {
            String url = person.getPhotoUrl();
            if (url == null || url.trim().isEmpty()) {
                continue;
            }

            totalChecked++;
            PhotoUrlValidator.ValidationResult result = photoUrlValidator.validatePhotoUrl(url);

            if (result.isValid()) {
                accessibleCount++;
                log.info("Audited photo URL for deceased ID {}: ACCESSIBLE (resolved to: {})",
                        person.getId(), result.getResolvedUrl());
            } else {
                inaccessibleCount++;
                log.warn("Audited photo URL for deceased ID {}: INACCESSIBLE - reason: '{}', url: '{}'",
                        person.getId(), result.getErrorMessage(), url);

                inaccessibleList.add(InaccessiblePhotoRecordDto.builder()
                        .deceasedPersonId(person.getId())
                        .deceasedName(person.getFullName())
                        .graveId(person.getGrave() != null ? person.getGrave().getId() : null)
                        .photoUrl(url)
                        .reason(result.getErrorMessage())
                        .build());
            }
        }

        log.info("Photo URL audit complete: total checked={}, accessible={}, inaccessible={}",
                totalChecked, accessibleCount, inaccessibleCount);

        return PhotoAuditReportDto.builder()
                .totalChecked(totalChecked)
                .accessibleCount(accessibleCount)
                .inaccessibleCount(inaccessibleCount)
                .inaccessibleRecords(inaccessibleList)
                .build();
    }
}
