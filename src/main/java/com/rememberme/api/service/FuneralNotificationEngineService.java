package com.rememberme.api.service;

import com.rememberme.api.dto.request.VipBroadcastRequest;
import com.rememberme.api.dto.response.LocationPingResponse;
import com.rememberme.api.entity.*;
import com.rememberme.api.repository.FuneralEventRepository;
import com.rememberme.api.repository.RememberMeRepository;
import com.rememberme.api.repository.UserNotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FuneralNotificationEngineService {

    private final UserNotificationPreferenceRepository preferenceRepository;
    private final FuneralEventRepository funeralEventRepository;
    private final RememberMeRepository rememberMeRepository;
    private final FirebaseNotificationService firebaseNotificationService;
    private final NotificationPreferenceService preferenceService;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double FAMOUS_GRAVE_PROXIMITY_RADIUS_KM = 3.0;

    /// Dispatches notifications for a newly created or updated funeral event
    public int dispatchNearbyFuneralNotifications(FuneralEvent funeral) {
        List<UserNotificationPreference> candidates = preferenceRepository.findAllActiveNearbyCandidates();
        int notificationsSent = 0;

        for (UserNotificationPreference pref : candidates) {
            if (isReligionMatched(pref, funeral.getReligion())) {
                double distanceKm = calculateDistanceKm(
                        pref.getLastKnownLatitude(), pref.getLastKnownLongitude(),
                        funeral.getLatitude(), funeral.getLongitude());

                if (distanceKm <= pref.getNearbyRadiusKm()) {
                    String title = "Funeral Notice: " + funeral.getDeceasedPerson().getFullName();
                    String body = String.format("A funeral is taking place %.1f km from you at %s.",
                            distanceKm, funeral.getGraveyard() != null ? funeral.getGraveyard().getName() : "nearby location");

                    FirebaseNotificationService.SendResult result = firebaseNotificationService.sendToToken(
                            pref.getUser().getFcmToken(), title, body);

                    if (result.sent()) {
                        notificationsSent++;
                    }
                }
            }
        }

        log.info("Dispatched {} nearby funeral notifications for funeral ID {}", notificationsSent, funeral.getId());
        return notificationsSent;
    }

    /// Processes live location update from device, checks for famous graves and active nearby funerals
    public LocationPingResponse processLocationPing(User user, Double latitude, Double longitude) {
        UserNotificationPreference pref = preferenceService.updateLocation(user, latitude, longitude);

        // 1. Check for famous/historical graveyards nearby
        List<RememberMe> famousGraveyards = rememberMeRepository.findByIsFamousTrueOrIsHistoricalTrue();
        List<LocationPingResponse.FamousGraveyardSummaryDto> famousNearby = new ArrayList<>();

        for (RememberMe graveyard : famousGraveyards) {
            double dist = calculateDistanceKm(latitude, longitude, graveyard.getLatitude(), graveyard.getLongitude());
            if (dist <= FAMOUS_GRAVE_PROXIMITY_RADIUS_KM) {
                famousNearby.add(LocationPingResponse.FamousGraveyardSummaryDto.builder()
                        .id(graveyard.getId())
                        .name(graveyard.getName())
                        .city(graveyard.getCity())
                        .country(graveyard.getCountry())
                        .distanceKm(Math.round(dist * 10.0) / 10.0)
                        .historicalDetails(graveyard.getHistoricalDetails())
                        .build());
            }
        }

        // 2. Count active upcoming funerals within user's radius
        List<FuneralEvent> upcomingFunerals = funeralEventRepository.findUpcomingFunerals(LocalDateTime.now().minusHours(2));
        int nearbyCount = 0;
        for (FuneralEvent funeral : upcomingFunerals) {
            if (isReligionMatched(pref, funeral.getReligion())) {
                double dist = calculateDistanceKm(latitude, longitude, funeral.getLatitude(), funeral.getLongitude());
                if (dist <= pref.getNearbyRadiusKm()) {
                    nearbyCount++;
                }
            }
        }

        // 3. Send push notification if user has famous graves enabled and famous grave is in close proximity
        if (pref.isFamousGravesEnabled() && !famousNearby.isEmpty() && pref.getUser().getFcmToken() != null) {
            LocationPingResponse.FamousGraveyardSummaryDto closest = famousNearby.get(0);
            firebaseNotificationService.sendToToken(
                    pref.getUser().getFcmToken(),
                    "Famous Site Nearby: " + closest.getName(),
                    String.format("You are %.1f km away from %s.", closest.getDistanceKm(), closest.getName())
            );
        }

        return LocationPingResponse.builder()
                .nearbyFuneralsCount(nearbyCount)
                .famousGravesNearby(famousNearby)
                .build();
    }

    /// Broadcasts a VIP / National Funeral notification to eligible users across a region/country
    public int broadcastVipFuneral(VipBroadcastRequest request) {
        List<UserNotificationPreference> candidates = preferenceRepository.findAllActiveVipCandidates(request.getTargetCountry());
        int notificationsSent = 0;

        for (UserNotificationPreference pref : candidates) {
            FirebaseNotificationService.SendResult result = firebaseNotificationService.sendToToken(
                    pref.getUser().getFcmToken(), request.getTitle(), request.getBody());
            if (result.sent()) {
                notificationsSent++;
            }
        }

        log.info("Broadcasted VIP notification to {} users (Target Country: {})", notificationsSent, request.getTargetCountry());
        return notificationsSent;
    }

    private boolean isReligionMatched(UserNotificationPreference pref, Religion funeralReligion) {
        return pref.getReligions().contains(Religion.ALL) || pref.getReligions().contains(funeralReligion);
    }

    /// Haversine distance formula calculation in kilometers
    public double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
