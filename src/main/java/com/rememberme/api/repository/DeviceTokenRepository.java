package com.rememberme.api.repository;

import com.rememberme.api.entity.DeviceToken;
import com.rememberme.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    List<DeviceToken> findByUserId(Long userId);
    List<DeviceToken> findByUser(User user);
    Optional<DeviceToken> findByUserIdAndDeviceToken(Long userId, String deviceToken);
    Optional<DeviceToken> findByDeviceToken(String deviceToken);
    void deleteByDeviceToken(String deviceToken);
    void deleteByUserIdAndDeviceToken(Long userId, String deviceToken);
}
