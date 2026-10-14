package com.rememberme.api.service;

import com.rememberme.api.dto.response.NotificationResponseDto;
import com.rememberme.api.entity.*;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.DeviceTokenRepository;
import com.rememberme.api.repository.EventRepository;
import com.rememberme.api.repository.NotificationRepository;
import com.rememberme.api.repository.OrganizationAdminRepository;
import com.rememberme.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostcodeNotificationMatchingTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostcodeService postcodeService;

    @Mock
    private FirebaseNotificationService firebaseNotificationService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrganizationAdminRepository organizationAdminRepository;

    private NotificationManagementService notificationManagementService;
    private EventService eventService;

    @BeforeEach
    public void setUp() {
        notificationManagementService = new NotificationManagementService(
                deviceTokenRepository,
                notificationRepository,
                userRepository,
                postcodeService,
                firebaseNotificationService,
                eventRepository
        );

        eventService = new EventService(
                eventRepository,
                organizationAdminRepository,
                userRepository,
                notificationManagementService
        );
    }

    @Test
    public void postcodeMatching_MatchingUsersReceiveNotifications_NonmatchingDoNot() {
        Organization organization = Organization.builder()
                .id(1L)
                .name("East London Mosque")
                .postcode("E1 6AN")
                .approvalStatus(OrganizationApprovalStatus.APPROVED)
                .build();

        Event event = Event.builder()
                .id(10L)
                .organization(organization)
                .title("Funeral Announcement")
                .description("Service at 2 PM")
                .eventType(EventType.FUNERAL)
                .status(EventStatus.PUBLISHED)
                .build();

        // 3 matching users with different casings/spacings of E1 6AN (normalized to E16AN)
        User user1 = User.builder().id(101L).email("user1@test.com").postcode("E1 6AN").postcodeNormalized("E16AN").build();
        User user2 = User.builder().id(102L).email("user2@test.com").postcode("e1 6an").postcodeNormalized("E16AN").build();
        User user3 = User.builder().id(103L).email("user3@test.com").postcode("E16AN").postcodeNormalized("E16AN").build();

        // Device tokens for user 1 and user 2
        DeviceToken token1 = DeviceToken.builder().id(1L).user(user1).deviceToken("fcm_token_1").build();
        DeviceToken token2 = DeviceToken.builder().id(2L).user(user2).deviceToken("fcm_token_2").build();

        when(postcodeService.normalizePostcode("E1 6AN")).thenReturn("E16AN");
        when(userRepository.findByPostcodeNormalized("E16AN")).thenReturn(List.of(user1, user2, user3));

        when(notificationRepository.existsByEventIdAndUserId(eq(10L), any())).thenReturn(false);

        when(deviceTokenRepository.findByUserId(101L)).thenReturn(List.of(token1));
        when(deviceTokenRepository.findByUserId(102L)).thenReturn(List.of(token2));
        when(deviceTokenRepository.findByUserId(103L)).thenReturn(List.of()); // No device token

        when(firebaseNotificationService.sendToToken(eq("fcm_token_1"), any(), any(), any()))
                .thenReturn(FirebaseNotificationService.SendResult.sent("msg-1"));
        when(firebaseNotificationService.sendToToken(eq("fcm_token_2"), any(), any(), any()))
                .thenReturn(FirebaseNotificationService.SendResult.sent("msg-2"));

        int sentCount = notificationManagementService.processEventNotifications(event);

        assertEquals(3, sentCount);

        // Verify notification records created for all 3 matching users
        verify(notificationRepository, times(3)).save(any(Notification.class));

        // Verify Firebase push sent to user 1 and user 2
        verify(firebaseNotificationService, times(1)).sendToToken(eq("fcm_token_1"), eq("Funeral Announcement"), any(), any());
        verify(firebaseNotificationService, times(1)).sendToToken(eq("fcm_token_2"), eq("Funeral Announcement"), any(), any());

        // Verify no push sent for nonmatching users (not in matching list)
    }

    @Test
    public void postcodeMatching_NoMatchingUsers_NoNotificationsSent() {
        Organization organization = Organization.builder()
                .id(2L)
                .name("St. Peter Church")
                .postcode("SW1A 1AA")
                .build();

        Event event = Event.builder()
                .id(20L)
                .organization(organization)
                .title("Sunday Service")
                .build();

        when(postcodeService.normalizePostcode("SW1A 1AA")).thenReturn("SW1A1AA");
        when(userRepository.findByPostcodeNormalized("SW1A1AA")).thenReturn(List.of());

        int count = notificationManagementService.processEventNotifications(event);

        assertEquals(0, count);
        verify(notificationRepository, never()).save(any());
        verify(firebaseNotificationService, never()).sendToToken(any(), any(), any(), any());
    }

    @Test
    public void duplicateEventPublish_PreventedAndIdempotent() {
        User admin = User.builder().id(1L).email("admin@test.com").build();
        Organization organization = Organization.builder()
                .id(1L)
                .approvalStatus(OrganizationApprovalStatus.APPROVED)
                .build();
        OrganizationAdmin orgAdmin = OrganizationAdmin.builder().user(admin).organization(organization).build();

        Event alreadyPublishedEvent = Event.builder()
                .id(99L)
                .organization(organization)
                .status(EventStatus.PUBLISHED)
                .build();

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(organizationAdminRepository.findByUserId(1L)).thenReturn(Optional.of(orgAdmin));
        when(eventRepository.findById(99L)).thenReturn(Optional.of(alreadyPublishedEvent));

        ApiException ex = assertThrows(ApiException.class, () -> eventService.publishEvent("admin@test.com", 99L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("already published"));
    }

    @Test
    public void registerDeviceToken_AvoidsDuplicates() {
        User user = User.builder().id(10L).build();
        when(deviceTokenRepository.findByDeviceToken("fcm_token_abc")).thenReturn(Optional.empty());
        when(deviceTokenRepository.findByUserIdAndDeviceToken(10L, "fcm_token_abc")).thenReturn(Optional.empty());

        notificationManagementService.registerDeviceToken(user, "fcm_token_abc");

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository, times(1)).save(captor.capture());
        assertEquals("fcm_token_abc", captor.getValue().getDeviceToken());
        assertEquals(10L, captor.getValue().getUser().getId());
    }

    @Test
    public void getUserNotifications_ReturnsPaginatedList() {
        User user = User.builder().id(10L).build();
        Organization org = Organization.builder().name("Test Org").build();
        Event event = Event.builder().id(1L).title("Test Title").description("Test Desc").organization(org).eventType(EventType.FUNERAL).build();
        Notification notification = Notification.builder()
                .id(50L)
                .event(event)
                .user(user)
                .title("Test Title")
                .message("Test Desc")
                .isRead(false)
                .deliveryStatus("SENT")
                .build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of(notification)));

        Page<NotificationResponseDto> page = notificationManagementService.getUserNotifications(user, PageRequest.of(0, 10));

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        NotificationResponseDto dto = page.getContent().get(0);
        assertEquals(50L, dto.getNotificationId());
        assertEquals(1L, dto.getEventId());
        assertEquals("Test Org", dto.getOrganizationName());
        assertEquals("Test Title", dto.getEventTitle());
    }

    @Test
    public void postcodeMatching_WestminsterAbbey_EventId2_CreatesNotificationForResidentWithMatchingPostcode() {
        Organization westminsterAbbey = Organization.builder()
                .id(2L)
                .name("Westminster Abbey")
                .postcode("SW1P 3PA")
                .postcodeNormalized("SW1P3PA")
                .approvalStatus(OrganizationApprovalStatus.APPROVED)
                .build();

        Event event2 = Event.builder()
                .id(2L)
                .organization(westminsterAbbey)
                .title("Special Service at Westminster Abbey")
                .description("Memorial ceremony at the Abbey")
                .eventType(EventType.RELIGIOUS_FUNCTION)
                .status(EventStatus.PUBLISHED)
                .build();

        User resident = User.builder()
                .id(501L)
                .fullName("Westminster Resident")
                .email("resident@westminster.gov.uk")
                .postcode("sw1p 3pa") // lowercase with space
                .postcodeNormalized("SW1P3PA")
                .build();

        when(postcodeService.normalizePostcode("SW1P 3PA")).thenReturn("SW1P3PA");
        when(userRepository.findMatchingResidentsByPostcode("SW1P3PA")).thenReturn(List.of(resident));
        when(notificationRepository.existsByEventIdAndUserId(2L, 501L)).thenReturn(false);

        int count = notificationManagementService.processEventNotifications(event2);

        assertEquals(1, count);
        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        Notification savedNotif = notifCaptor.getValue();
        assertEquals(2L, savedNotif.getEvent().getId());
        assertEquals(501L, savedNotif.getUser().getId());
        assertEquals("Special Service at Westminster Abbey", savedNotif.getTitle());
        assertEquals("Memorial ceremony at the Abbey", savedNotif.getMessage());
        assertFalse(savedNotif.isRead());
    }

    @Test
    public void syncMissingNotifications_ResidentRegisteredAfterPublication_ReceivesExistingPublishedEvent() {
        Organization westminsterAbbey = Organization.builder()
                .id(2L)
                .name("Westminster Abbey")
                .postcode("SW1P 3PA")
                .postcodeNormalized("SW1P3PA")
                .build();

        Event publishedEvent2 = Event.builder()
                .id(2L)
                .organization(westminsterAbbey)
                .title("Westminster Memorial Event")
                .description("Annual remembrance")
                .eventType(EventType.FUNERAL)
                .status(EventStatus.PUBLISHED)
                .build();

        User newResident = User.builder()
                .id(777L)
                .fullName("New Resident")
                .email("newresident@london.co.uk")
                .postcode("SW1P 3PA")
                .build();

        when(postcodeService.normalizePostcode("SW1P 3PA")).thenReturn("SW1P3PA");
        when(eventRepository.findPublishedEventsByOrganizationPostcode("SW1P3PA"))
                .thenReturn(List.of(publishedEvent2));
        when(notificationRepository.existsByEventIdAndUserId(2L, 777L)).thenReturn(false);

        // When the resident retrieves their notifications, sync runs automatically
        Notification createdNotification = Notification.builder()
                .id(901L)
                .event(publishedEvent2)
                .user(newResident)
                .title("Westminster Memorial Event")
                .message("Annual remembrance")
                .build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(777L), any()))
                .thenReturn(new PageImpl<>(List.of(createdNotification)));

        Page<NotificationResponseDto> result = notificationManagementService.getUserNotifications(
                newResident, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(2L, result.getContent().get(0).getEventId());
        assertEquals("Westminster Abbey", result.getContent().get(0).getOrganizationName());
        assertEquals("Westminster Memorial Event", result.getContent().get(0).getEventTitle());

        // Verify the notification record was persisted for the resident
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    public void postcodeMatching_DifferentPostcode_DoesNotReceiveNotification() {
        Organization westminsterAbbey = Organization.builder()
                .id(2L)
                .name("Westminster Abbey")
                .postcode("SW1P 3PA")
                .build();

        Event event2 = Event.builder()
                .id(2L)
                .organization(westminsterAbbey)
                .title("Westminster Service")
                .status(EventStatus.PUBLISHED)
                .build();

        User differentResident = User.builder()
                .id(888L)
                .email("camden@test.com")
                .postcode("NW1 4NP") // Different postcode
                .postcodeNormalized("NW14NP")
                .build();

        when(postcodeService.normalizePostcode("SW1P 3PA")).thenReturn("SW1P3PA");
        when(userRepository.findMatchingResidentsByPostcode("SW1P3PA")).thenReturn(List.of());

        int count = notificationManagementService.processEventNotifications(event2);

        assertEquals(0, count);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    public void syncMissingNotifications_Deduplication_NeverCreatesDuplicates() {
        Organization westminsterAbbey = Organization.builder()
                .id(2L)
                .name("Westminster Abbey")
                .postcode("SW1P 3PA")
                .build();

        Event publishedEvent2 = Event.builder()
                .id(2L)
                .organization(westminsterAbbey)
                .status(EventStatus.PUBLISHED)
                .build();

        User resident = User.builder()
                .id(501L)
                .postcode("SW1P 3PA")
                .postcodeNormalized("SW1P3PA")
                .build();

        when(postcodeService.normalizePostcode("SW1P 3PA")).thenReturn("SW1P3PA");
        when(eventRepository.findPublishedEventsByOrganizationPostcode("SW1P3PA"))
                .thenReturn(List.of(publishedEvent2));
        // Notification already exists in repository
        when(notificationRepository.existsByEventIdAndUserId(2L, 501L)).thenReturn(true);

        int synced = notificationManagementService.syncMissingNotificationsForUser(resident);

        assertEquals(0, synced);
        verify(notificationRepository, never()).save(any());
    }
}
