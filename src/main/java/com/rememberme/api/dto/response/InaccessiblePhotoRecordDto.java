package com.rememberme.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InaccessiblePhotoRecordDto {
    private Long deceasedPersonId;
    private String deceasedName;
    private Long graveId;
    private String photoUrl;
    private String reason;
}
