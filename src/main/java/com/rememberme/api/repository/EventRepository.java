package com.rememberme.api.repository;

import com.rememberme.api.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByOrganizationId(Long organizationId);
    List<Event> findByOrganizationIdOrderByEventDateTimeDesc(Long organizationId);
    Optional<Event> findByIdAndOrganizationId(Long id, Long organizationId);
}
