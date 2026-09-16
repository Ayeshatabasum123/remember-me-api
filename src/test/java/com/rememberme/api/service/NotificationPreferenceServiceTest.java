package com.rememberme.api.service;

import com.rememberme.api.dto.request.NotificationPreferencesDto;
import com.rememberme.api.entity.Religion;
import com.rememberme.api.entity.User;
import com.rememberme.api.entity.UserNotificationPreference;
import com.rememberme.api.repository.UserNotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private UserNotificationPreferenceRepository preferenceRepository;

    private NotificationPreferenceService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new NotificationPreferenceService(preferenceRepository);
        user = User.builder().id(1L).email("user@example.com").build();
    }

    @Test
    void getPreferences_CreatesDefaultIfMissing() {
        when(preferenceRepository.findByUser(user)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationPreferencesDto result = service.getPreferences(user);

        assertTrue(result.getNearbyFuneralsEnabled());
        assertEquals(10.0, result.getNearbyRadiusKm());
        assertTrue(result.getReligions().contains(Religion.ALL));
        assertTrue(result.getFamousGravesEnabled());
        assertTrue(result.getVipFuneralsEnabled());
    }

    @Test
    void updatePreferences_UpdatesAndPersistsPreferences() {
        UserNotificationPreference existing = UserNotificationPreference.builder()
                .id(10L)
                .user(user)
                .nearbyFuneralsEnabled(true)
                .nearbyRadiusKm(10.0)
                .build();

        when(preferenceRepository.findByUser(user)).thenReturn(Optional.of(existing));
        when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationPreferencesDto updateDto = NotificationPreferencesDto.builder()
                .nearbyFuneralsEnabled(false)
                .nearbyRadiusKm(25.0)
                .religions(Set.of(Religion.ISLAM, Religion.CHRISTIANITY))
                .famousGravesEnabled(true)
                .vipFuneralsEnabled(false)
                .build();

        NotificationPreferencesDto result = service.updatePreferences(user, updateDto);

        assertFalse(result.getNearbyFuneralsEnabled());
        assertEquals(25.0, result.getNearbyRadiusKm());
        assertEquals(2, result.getReligions().size());
        assertFalse(result.getVipFuneralsEnabled());
    }
}
