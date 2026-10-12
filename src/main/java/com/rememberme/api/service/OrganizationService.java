package com.rememberme.api.service;

import com.rememberme.api.dto.request.AdminRegisterRequest;
import com.rememberme.api.dto.response.AdminRegisterResponse;
import com.rememberme.api.dto.response.OrganizationResponseDto;
import com.rememberme.api.dto.response.PostcodeValidationResponse;
import com.rememberme.api.entity.Organization;
import com.rememberme.api.entity.OrganizationAdmin;
import com.rememberme.api.entity.OrganizationApprovalStatus;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.OrganizationAdminRepository;
import com.rememberme.api.repository.OrganizationRepository;
import com.rememberme.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationAdminRepository organizationAdminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PostcodeService postcodeService;
    private final FileStorageService fileStorageService;
    private final OrganizationCodeGeneratorService codeGeneratorService;

    @Transactional
    public AdminRegisterResponse registerAdminAndOrganization(AdminRegisterRequest request, MultipartFile photographFile) {
        if (!StringUtils.hasText(request.getOrganizationName())) {
            throw new ApiException("Organization name must not be empty", HttpStatus.BAD_REQUEST);
        }

        if (!StringUtils.hasText(request.getEmail())) {
            throw new ApiException("Email is required", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(request.getEmail()) || organizationRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email is already registered", HttpStatus.BAD_REQUEST);
        }

        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new ApiException("Password must contain at least 8 characters", HttpStatus.BAD_REQUEST);
        }

        if (request.getConfirmPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApiException("Password and confirm password must match", HttpStatus.BAD_REQUEST);
        }

        if (request.getLatitude() == null || request.getLatitude() < -90.0 || request.getLatitude() > 90.0) {
            throw new ApiException("Latitude must be between -90 and 90", HttpStatus.BAD_REQUEST);
        }

        if (request.getLongitude() == null || request.getLongitude() < -180.0 || request.getLongitude() > 180.0) {
            throw new ApiException("Longitude must be between -180 and 180", HttpStatus.BAD_REQUEST);
        }

        // Postcode validation via Postcodes.io
        PostcodeValidationResponse postcodeResult = postcodeService.validatePostcode(request.getPostcode());
        if (!postcodeResult.isValid()) {
            throw new ApiException("Invalid UK postcode: " + request.getPostcode(), HttpStatus.BAD_REQUEST);
        }

        String canonicalPostcode = postcodeResult.getPostcode();
        String normalizedPostcode = postcodeService.normalizePostcode(canonicalPostcode);

        // Photo upload / resolution
        String photoUrl;
        MultipartFile effectiveFile = photographFile != null ? photographFile : request.getPhotograph();
        if (effectiveFile != null && !effectiveFile.isEmpty()) {
            photoUrl = fileStorageService.storeOrganizationPhoto(effectiveFile);
        } else if (StringUtils.hasText(request.getPhotoUrl())) {
            photoUrl = request.getPhotoUrl();
        } else {
            throw new ApiException("Organization photograph is required", HttpStatus.BAD_REQUEST);
        }

        // 1. Create Admin User
        User adminUser = User.builder()
                .fullName(request.getOrganizationName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.ADMIN)
                .postcode(canonicalPostcode)
                .postcodeNormalized(normalizedPostcode)
                .photoUrl(photoUrl)
                .enabled(true)
                .emailVerified(true)
                .phoneVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(adminUser);

        // 2. Create Organization in PENDING status
        Organization organization = Organization.builder()
                .name(request.getOrganizationName())
                .organizationType(request.getOrganizationType())
                .email(request.getEmail())
                .phone(request.getPhone())
                .postcode(canonicalPostcode)
                .postcodeNormalized(normalizedPostcode)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .photoUrl(photoUrl)
                .organizationCode(null)
                .approvalStatus(OrganizationApprovalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        organizationRepository.save(organization);

        // 3. Link Admin to Organization
        OrganizationAdmin orgAdmin = OrganizationAdmin.builder()
                .organization(organization)
                .user(adminUser)
                .createdAt(LocalDateTime.now())
                .build();
        organizationAdminRepository.save(orgAdmin);

        log.info("Registered religious organization '{}' with ID {} in PENDING status for admin user '{}'",
                organization.getName(), organization.getId(), adminUser.getEmail());

        return AdminRegisterResponse.builder()
                .userId(adminUser.getId())
                .email(adminUser.getEmail())
                .fullName(adminUser.getFullName())
                .role(adminUser.getRole().name())
                .organization(OrganizationResponseDto.fromEntity(organization))
                .message("Organization registered successfully. Approval is currently PENDING.")
                .build();
    }

    public List<OrganizationResponseDto> getPendingOrganizations() {
        return organizationRepository.findByApprovalStatus(OrganizationApprovalStatus.PENDING)
                .stream()
                .map(OrganizationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrganizationResponseDto approveOrganization(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ApiException("Organization not found with ID: " + id, HttpStatus.NOT_FOUND));

        organization.setApprovalStatus(OrganizationApprovalStatus.APPROVED);
        if (!StringUtils.hasText(organization.getOrganizationCode())) {
            String orgCode = codeGeneratorService.generateNextCode();
            organization.setOrganizationCode(orgCode);
        }
        organization.setUpdatedAt(LocalDateTime.now());
        organizationRepository.save(organization);

        log.info("Super Admin approved organization '{}' (ID: {}). Assigned code: {}",
                organization.getName(), organization.getId(), organization.getOrganizationCode());

        return OrganizationResponseDto.fromEntity(organization);
    }

    @Transactional
    public OrganizationResponseDto rejectOrganization(Long id, String rejectionReason) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ApiException("Organization not found with ID: " + id, HttpStatus.NOT_FOUND));

        organization.setApprovalStatus(OrganizationApprovalStatus.REJECTED);
        organization.setRejectionReason(rejectionReason);
        organization.setUpdatedAt(LocalDateTime.now());
        organizationRepository.save(organization);

        log.info("Super Admin rejected organization '{}' (ID: {}). Reason: {}",
                organization.getName(), organization.getId(), rejectionReason);

        return OrganizationResponseDto.fromEntity(organization);
    }
}
