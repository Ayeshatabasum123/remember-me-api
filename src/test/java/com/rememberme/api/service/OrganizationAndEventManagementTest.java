package com.rememberme.api.service;

import com.rememberme.api.dto.request.AdminRegisterRequest;
import com.rememberme.api.dto.request.CreateEventRequest;
import com.rememberme.api.dto.request.UpdateEventRequest;
import com.rememberme.api.dto.response.AdminRegisterResponse;
import com.rememberme.api.dto.response.EventResponseDto;
import com.rememberme.api.dto.response.OrganizationResponseDto;
import com.rememberme.api.dto.response.PostcodeValidationResponse;
import com.rememberme.api.entity.*;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.EventRepository;
import com.rememberme.api.repository.OrganizationAdminRepository;
import com.rememberme.api.repository.OrganizationRepository;
import com.rememberme.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrganizationAndEventManagementTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationAdminRepository organizationAdminRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PostcodeService postcodeService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private OrganizationCodeGeneratorService codeGeneratorService;

    @Mock
    private NotificationManagementService notificationManagementService;

    private OrganizationService organizationService;
    private EventService eventService;

    @BeforeEach
    public void setUp() {
        organizationService = new OrganizationService(
                organizationRepository,
                organizationAdminRepository,
                userRepository,
                passwordEncoder,
                postcodeService,
                fileStorageService,
                codeGeneratorService
        );

        eventService = new EventService(
                eventRepository,
                organizationAdminRepository,
                userRepository,
                notificationManagementService
        );
    }

    @Test
    public void registerAdminAndOrganization_InitialStatusIsPending() {
        AdminRegisterRequest request = AdminRegisterRequest.builder()
                .organizationName("Grace Church")
                .organizationType(OrganizationType.CHURCH)
                .email("admin@gracechurch.org")
                .phone("+442079460000")
                .password("Password123")
                .confirmPassword("Password123")
                .postcode("E1 6AN")
                .latitude(51.52)
                .longitude(-0.07)
                .photoUrl("https://example.com/church.jpg")
                .build();

        when(userRepository.existsByEmail("admin@gracechurch.org")).thenReturn(false);
        when(organizationRepository.existsByEmail("admin@gracechurch.org")).thenReturn(false);
        when(postcodeService.validatePostcode("E1 6AN")).thenReturn(
                PostcodeValidationResponse.builder().valid(true).postcode("E1 6AN").build()
        );
        when(postcodeService.normalizePostcode("E1 6AN")).thenReturn("E16AN");
        when(passwordEncoder.encode("Password123")).thenReturn("encoded_pass");

        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            return u;
        });

        when(organizationRepository.save(any(Organization.class))).thenAnswer(i -> {
            Organization o = i.getArgument(0);
            o.setId(100L);
            return o;
        });

        AdminRegisterResponse response = organizationService.registerAdminAndOrganization(request, null);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals("ADMIN", response.getRole());
        assertEquals(OrganizationApprovalStatus.PENDING, response.getOrganization().getApprovalStatus());
        assertNull(response.getOrganization().getOrganizationCode());
    }

    @Test
    public void superAdminApprove_GeneratesUniqueOrganizationCode() {
        Organization pendingOrg = Organization.builder()
                .id(100L)
                .name("Grace Church")
                .approvalStatus(OrganizationApprovalStatus.PENDING)
                .build();

        when(organizationRepository.findById(100L)).thenReturn(Optional.of(pendingOrg));
        when(codeGeneratorService.generateNextCode()).thenReturn("ORG-000001");
        when(organizationRepository.save(any(Organization.class))).thenAnswer(i -> i.getArgument(0));

        OrganizationResponseDto approved = organizationService.approveOrganization(100L);

        assertEquals(OrganizationApprovalStatus.APPROVED, approved.getApprovalStatus());
        assertEquals("ORG-000001", approved.getOrganizationCode());
    }

    @Test
    public void superAdminReject_UpdatesStatusAndReason() {
        Organization pendingOrg = Organization.builder()
                .id(100L)
                .name("Grace Church")
                .approvalStatus(OrganizationApprovalStatus.PENDING)
                .build();

        when(organizationRepository.findById(100L)).thenReturn(Optional.of(pendingOrg));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(i -> i.getArgument(0));

        OrganizationResponseDto rejected = organizationService.rejectOrganization(100L, "Invalid charity registration");

        assertEquals(OrganizationApprovalStatus.REJECTED, rejected.getApprovalStatus());
        assertEquals("Invalid charity registration", rejected.getRejectionReason());
    }

    @Test
    public void pendingAdmin_UnableToCreateEvent() {
        User admin = User.builder().id(1L).email("admin@gracechurch.org").build();
        Organization pendingOrg = Organization.builder()
                .id(100L)
                .approvalStatus(OrganizationApprovalStatus.PENDING)
                .build();
        OrganizationAdmin orgAdmin = OrganizationAdmin.builder().user(admin).organization(pendingOrg).build();

        when(userRepository.findByEmail("admin@gracechurch.org")).thenReturn(Optional.of(admin));
        when(organizationAdminRepository.findByUserId(1L)).thenReturn(Optional.of(orgAdmin));

        CreateEventRequest request = CreateEventRequest.builder()
                .title("Funeral Service")
                .eventType(EventType.FUNERAL)
                .eventDateTime(OffsetDateTime.now())
                .build();

        ApiException ex = assertThrows(ApiException.class, () -> eventService.createEvent("admin@gracechurch.org", request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Organization is not approved yet"));
    }

    @Test
    public void approvedAdmin_AbleToCreateAndPublishEvent() {
        User admin = User.builder().id(1L).email("admin@gracechurch.org").build();
        Organization approvedOrg = Organization.builder()
                .id(100L)
                .name("Grace Church")
                .postcode("E1 6AN")
                .approvalStatus(OrganizationApprovalStatus.APPROVED)
                .build();
        OrganizationAdmin orgAdmin = OrganizationAdmin.builder().user(admin).organization(approvedOrg).build();

        when(userRepository.findByEmail("admin@gracechurch.org")).thenReturn(Optional.of(admin));
        when(organizationAdminRepository.findByUserId(1L)).thenReturn(Optional.of(orgAdmin));

        Event draftEvent = Event.builder()
                .id(50L)
                .organization(approvedOrg)
                .title("Funeral Service")
                .eventType(EventType.FUNERAL)
                .eventDateTime(OffsetDateTime.now())
                .status(EventStatus.DRAFT)
                .build();

        when(eventRepository.findById(50L)).thenReturn(Optional.of(draftEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        EventResponseDto published = eventService.publishEvent("admin@gracechurch.org", 50L);

        assertEquals(EventStatus.PUBLISHED, published.getStatus());
        verify(notificationManagementService, times(1)).processEventNotifications(draftEvent);
    }

    @Test
    public void adminUnableToEditAnotherOrganizationsEvent() {
        User admin = User.builder().id(1L).email("admin@gracechurch.org").build();
        Organization org1 = Organization.builder().id(100L).approvalStatus(OrganizationApprovalStatus.APPROVED).build();
        Organization org2 = Organization.builder().id(200L).approvalStatus(OrganizationApprovalStatus.APPROVED).build();
        OrganizationAdmin orgAdmin = OrganizationAdmin.builder().user(admin).organization(org1).build();

        when(userRepository.findByEmail("admin@gracechurch.org")).thenReturn(Optional.of(admin));
        when(organizationAdminRepository.findByUserId(1L)).thenReturn(Optional.of(orgAdmin));

        Event otherOrgEvent = Event.builder()
                .id(55L)
                .organization(org2)
                .title("Other Event")
                .build();

        when(eventRepository.findById(55L)).thenReturn(Optional.of(otherOrgEvent));

        UpdateEventRequest request = UpdateEventRequest.builder()
                .title("Tampered Title")
                .eventType(EventType.OTHER)
                .eventDateTime(OffsetDateTime.now())
                .build();

        ApiException ex = assertThrows(ApiException.class, () -> eventService.updateEvent("admin@gracechurch.org", 55L, request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("You cannot modify an event belonging to another organization"));
    }
}
