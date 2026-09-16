package com.rememberme.api.repository;

import com.rememberme.api.entity.RememberMe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RememberMeRepository extends JpaRepository<RememberMe, Long> {
    List<RememberMe> findByNameContainingIgnoreCase(String name);
    List<RememberMe> findByStatus(RememberMe.ApprovalStatus status);
    List<RememberMe> findByIsFamousTrueOrIsHistoricalTrue();
    boolean existsByNameIgnoreCaseAndLatitudeAndLongitude(String name, Double latitude, Double longitude);
}
