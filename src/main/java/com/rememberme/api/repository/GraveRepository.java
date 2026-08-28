package com.rememberme.api.repository;

import com.rememberme.api.entity.Grave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GraveRepository extends JpaRepository<Grave, Long> {
    boolean existsByRememberMeId(Long rememberMeId);
}
