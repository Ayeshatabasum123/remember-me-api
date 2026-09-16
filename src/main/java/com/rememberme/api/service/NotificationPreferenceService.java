package com.rememberme.api.service;

import com.rememberme.api.dto.request.NotificationPreferencesDto;
import com.rememberme.api.entity.Religion;
import com.rememberme.api.entity.User;
import com.rememberme.api.entity.UserNotificationPreference;
import com.rememberme.api.repository.UserNotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceService {

    private final UserNotificationPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public NotificationPreferencesDto getPreferences(User user) {
        UserNotificationPreference pref = getOrCreatePreference(user);
        return mapToDto(pref);
    }

    @Transactional
    public NotificationPreferencesDto updatePreferences(User user, NotificationPreferencesDto dto) {
        UserNotificationPreference pref = getOrCreatePreference(user);

        pref.setNearbyFuneralsEnabled(dto.getNearbyFuneralsEnabled());
        pref.setNearbyRadiusKm(dto.getNearbyRadiusKm());
        pref.setReligions(dto.getReligions() != null && !dto.getReligions().isEmpty()
                ? new HashSet<>(dto.getReligions())
                : new HashSet<>(Set.of(Religion.ALL)));
        pref.setFamousGravesEnabled(dto.getFamousGravesEnabled());
        pref.setVipFuneralsEnabled(dto.getVipFuneralsEnabled());

        UserNotificationPreference saved = preferenceRepository.save(pref);
        log.info("Updated notification preferences for user ID {}", user.getId());
        return mapToDto(saved);
    }

    @Transactional
    public UserNotificationPreference updateLocation(User user, Double latitude, Double longitude) {
        UserNotificationPreference pref = getOrCreatePreference(user);
        pref.setLastKnownLatitude(latitude);
        pref.setLastKnownLongitude(longitude);
        pref.setLastLocationUpdate(LocalDateTime.now());
        return preferenceRepository.save(pref);
    }

    @Transactional
    public UserNotificationPreference getOrCreatePreference(User user) {
        return preferenceRepository.findByUser(user)
                .orElseGet(() -> preferenceRepository.save(UserNotificationPreference.builder()
                        .user(user)
                        .nearbyFuneralsEnabled(true)
                        .nearbyRadiusKm(10.0)
                        .religions(new HashSet<>(Set.of(Religion.ALL)))
                        .famousGravesEnabled(true)
                        .vipFuneralsEnabled(true)
                        .build()));
    }

    private NotificationPreferencesDto mapToDto(UserNotificationPreference pref) {
        return NotificationPreferencesDto.builder()
                .nearbyFuneralsEnabled(pref.isNearbyFuneralsEnabled())
                .nearbyRadiusKm(pref.getNearbyRadiusKm())
                .religions(pref.getReligions())
                .famousGravesEnabled(pref.isFamousGravesEnabled())
                .vipFuneralsEnabled(pref.isVipFuneralsEnabled())
                .build();
    }
}
