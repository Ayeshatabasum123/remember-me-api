package com.rememberme.api.repository;

import com.rememberme.api.entity.Event;
import com.rememberme.api.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByOrganizationId(Long organizationId);
    List<Event> findByOrganizationIdOrderByEventDateTimeDesc(Long organizationId);
    Optional<Event> findByIdAndOrganizationId(Long id, Long organizationId);
    List<Event> findByStatus(EventStatus status);

    @Query("SELECT e FROM Event e WHERE e.status = com.rememberme.api.entity.EventStatus.PUBLISHED AND " +
           "((e.organization.postcodeNormalized IS NOT NULL AND e.organization.postcodeNormalized = :postcodeNorm) OR " +
           "(e.organization.postcode IS NOT NULL AND UPPER(REPLACE(e.organization.postcode, ' ', '')) = :postcodeNorm))")
    List<Event> findPublishedEventsByOrganizationPostcode(@Param("postcodeNorm") String postcodeNorm);
}
