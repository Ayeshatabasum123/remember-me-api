package com.rememberme.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoAuditReportDto {
    private int totalChecked;
    private int accessibleCount;
    private int inaccessibleCount;
    private List<InaccessiblePhotoRecordDto> inaccessibleRecords;
}
