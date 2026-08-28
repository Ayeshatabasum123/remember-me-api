package com.rememberme.api.repository;

import com.rememberme.api.entity.Memorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemorialRepository extends JpaRepository<Memorial, Long> {
    boolean existsByDeceasedPersonId(Long deceasedPersonId);
}
