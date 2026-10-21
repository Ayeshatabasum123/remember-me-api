package com.rememberme.api.communityfund.repository;

import com.rememberme.api.communityfund.entity.FundSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FundSettingsRepository extends JpaRepository<FundSettings, Long> {

    Optional<FundSettings> findTopByOrderByIdAsc();
}
