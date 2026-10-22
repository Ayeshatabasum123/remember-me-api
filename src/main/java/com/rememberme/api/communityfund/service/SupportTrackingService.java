package com.rememberme.api.communityfund.service;

import com.rememberme.api.communityfund.dto.request.SubmitAdditionalInfoDto;
import com.rememberme.api.communityfund.dto.response.SupportRequestResponseDto;
import com.rememberme.api.communityfund.dto.response.SupportRequestStatusResponseDto;
import com.rememberme.api.communityfund.dto.response.SupportRequestSummaryDto;
import com.rememberme.api.communityfund.dto.response.SupportRequestUpdateResponseDto;
import com.rememberme.api.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SupportTrackingService {

    Page<SupportRequestSummaryDto> getMyApplications(User applicant, int page, int size, String sortBy, String direction);

    SupportRequestStatusResponseDto getRequestStatus(Long requestId, User currentUser);

    List<SupportRequestUpdateResponseDto> getRequestUpdates(Long requestId, User currentUser);

    Page<SupportRequestUpdateResponseDto> getRequestHistory(Long requestId, User currentUser, int page, int size);

    SupportRequestResponseDto submitAdditionalInfo(Long requestId, SubmitAdditionalInfoDto dto, User currentUser);

    void recordStatusTransition(Long requestId, com.rememberme.api.communityfund.enums.SupportRequestStatus previousStatus,
                                com.rememberme.api.communityfund.enums.SupportRequestStatus newStatus,
                                String message, User updatedBy);
}
