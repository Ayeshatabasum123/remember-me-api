package com.rememberme.api.communityfund.service;

import com.rememberme.api.communityfund.dto.request.CreateSupportRequestDto;
import com.rememberme.api.communityfund.dto.request.UpdateSupportRequestDto;
import com.rememberme.api.communityfund.dto.response.SupportDocumentResponseDto;
import com.rememberme.api.communityfund.dto.response.SupportRequestCreateResponseDto;
import com.rememberme.api.communityfund.dto.response.SupportRequestResponseDto;
import com.rememberme.api.communityfund.enums.SupportDocumentType;
import com.rememberme.api.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SupportRequestService {

    SupportRequestCreateResponseDto createSupportRequest(User applicant, CreateSupportRequestDto dto, String idempotencyKey);

    SupportRequestResponseDto getSupportRequest(Long id, User currentUser);

    SupportRequestResponseDto updateSupportRequest(Long id, UpdateSupportRequestDto dto, User currentUser);

    SupportDocumentResponseDto uploadDocument(Long requestId, MultipartFile file, SupportDocumentType documentType, User currentUser);

    List<SupportDocumentResponseDto> listDocuments(Long requestId, User currentUser);

    DownloadedDocument downloadDocument(Long requestId, Long documentId, User currentUser);

    void deleteDocument(Long requestId, Long documentId, User currentUser);

    record DownloadedDocument(Resource resource, String originalFilename, String contentType, long fileSize) {}
}
