package com.rememberme.api.repository;

import com.rememberme.api.entity.User;
import com.rememberme.api.entity.UserNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, Long> {

    Optional<UserNotificationPreference> findByUser(User user);

    Optional<UserNotificationPreference> findByUserId(Long userId);

    @Query("SELECT p FROM UserNotificationPreference p WHERE p.nearbyFuneralsEnabled = true AND p.user.fcmToken IS NOT NULL AND p.lastKnownLatitude IS NOT NULL AND p.lastKnownLongitude IS NOT NULL")
    List<UserNotificationPreference> findAllActiveNearbyCandidates();

    @Query("SELECT p FROM UserNotificationPreference p WHERE p.vipFuneralsEnabled = true AND p.user.fcmToken IS NOT NULL AND (:country IS NULL OR LOWER(p.user.country) = LOWER(:country))")
    List<UserNotificationPreference> findAllActiveVipCandidates(@Param("country") String country);
}
