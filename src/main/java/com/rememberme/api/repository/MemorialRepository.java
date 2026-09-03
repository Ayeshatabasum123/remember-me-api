package com.rememberme.api.repository;

import com.rememberme.api.entity.Memorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemorialRepository extends JpaRepository<Memorial, Long> {
    boolean existsByDeceasedPersonId(Long deceasedPersonId);
    Optional<Memorial> findByDeceasedPersonId(Long deceasedPersonId);
    void deleteByDeceasedPersonId(Long deceasedPersonId);
}
