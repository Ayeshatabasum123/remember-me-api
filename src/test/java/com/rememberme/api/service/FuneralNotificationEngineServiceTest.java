package com.rememberme.api.service;

import com.rememberme.api.entity.*;
import com.rememberme.api.repository.FuneralEventRepository;
import com.rememberme.api.repository.RememberMeRepository;
import com.rememberme.api.repository.UserNotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuneralNotificationEngineServiceTest {

    @Mock
    private UserNotificationPreferenceRepository preferenceRepository;
    @Mock
    private FuneralEventRepository funeralEventRepository;
    @Mock
    private RememberMeRepository rememberMeRepository;
    @Mock
    private FirebaseNotificationService firebaseNotificationService;
    @Mock
    private NotificationPreferenceService preferenceService;

    private FuneralNotificationEngineService service;

    @BeforeEach
    void setUp() {
        service = new FuneralNotificationEngineService(
                preferenceRepository, funeralEventRepository, rememberMeRepository,
                firebaseNotificationService, preferenceService);
    }

    @Test
    void calculateDistanceKm_ComputesCorrectDistance() {
        // San Francisco to Los Angeles is ~559 km
        double distance = service.calculateDistanceKm(37.774929, -122.419416, 34.052235, -118.243683);
        assertTrue(distance > 550 && distance < 570);
    }

    @Test
    void dispatchNearbyFuneralNotifications_SendsOnlyToUsersInRadiusAndMatchingReligion() {
        User user1 = User.builder().id(1L).fcmToken("token1").build();
        UserNotificationPreference pref1 = UserNotificationPreference.builder()
                .user(user1)
                .nearbyFuneralsEnabled(true)
                .nearbyRadiusKm(10.0)
                .religions(Set.of(Religion.ISLAM))
                .lastKnownLatitude(37.7749)
                .lastKnownLongitude(-122.4194)
                .build();

        FuneralEvent funeral = FuneralEvent.builder()
                .id(100L)
                .deceasedPerson(DeceasedPerson.builder().fullName("John Doe").build())
                .religion(Religion.ISLAM)
                .latitude(37.7750) // ~0.01 km away
                .longitude(-122.4195)
                .build();

        when(preferenceRepository.findAllActiveNearbyCandidates()).thenReturn(List.of(pref1));
        when(firebaseNotificationService.sendToToken(any(), any(), any()))
                .thenReturn(FirebaseNotificationService.SendResult.sent("msg-1"));

        int sent = service.dispatchNearbyFuneralNotifications(funeral);

        assertEquals(1, sent);
        verify(firebaseNotificationService).sendToToken(eq("token1"), any(), any());
    }

    @Test
    void dispatchNearbyFuneralNotifications_SkipsUsersOutsideRadius() {
        User user1 = User.builder().id(1L).fcmToken("token1").build();
        UserNotificationPreference pref1 = UserNotificationPreference.builder()
                .user(user1)
                .nearbyFuneralsEnabled(true)
                .nearbyRadiusKm(5.0) // 5 km radius limit
                .religions(Set.of(Religion.ALL))
                .lastKnownLatitude(37.7749)
                .lastKnownLongitude(-122.4194)
                .build();

        FuneralEvent farFuneral = FuneralEvent.builder()
                .id(200L)
                .deceasedPerson(DeceasedPerson.builder().fullName("Jane Doe").build())
                .religion(Religion.CHRISTIANITY)
                .latitude(34.0522) // Los Angeles ~559 km away
                .longitude(-118.2436)
                .build();

        when(preferenceRepository.findAllActiveNearbyCandidates()).thenReturn(List.of(pref1));

        int sent = service.dispatchNearbyFuneralNotifications(farFuneral);

        assertEquals(0, sent);
        verify(firebaseNotificationService, never()).sendToToken(any(), any(), any());
    }
}
