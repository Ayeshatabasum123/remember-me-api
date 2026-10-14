package com.rememberme.api.service;

import com.rememberme.api.dto.request.CreateEventRequest;
import com.rememberme.api.dto.request.UpdateEventRequest;
import com.rememberme.api.dto.response.EventResponseDto;
import com.rememberme.api.entity.*;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.EventRepository;
import com.rememberme.api.repository.OrganizationAdminRepository;
import com.rememberme.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final OrganizationAdminRepository organizationAdminRepository;
    private final UserRepository userRepository;
    private final NotificationManagementService notificationManagementService;

    @Transactional
    public EventResponseDto createEvent(String adminEmail, CreateEventRequest request) {
        Organization organization = getAdminOrganization(adminEmail);

        if (organization.getApprovalStatus() != OrganizationApprovalStatus.APPROVED) {
            throw new ApiException("Organization is not approved yet. Admins cannot create events until approval.", HttpStatus.FORBIDDEN);
        }

        Event event = Event.builder()
                .organization(organization)
                .title(request.getTitle())
                .description(request.getDescription())
                .eventType(request.getEventType())
                .eventDateTime(request.getEventDateTime())
                .status(EventStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        eventRepository.save(event);
        log.info("Admin '{}' created draft event ID {} for organization '{}'",
                adminEmail, event.getId(), organization.getName());

        return EventResponseDto.fromEntity(event);
    }

    @Transactional
    public EventResponseDto updateEvent(String adminEmail, Long eventId, UpdateEventRequest request) {
        Organization organization = getAdminOrganization(adminEmail);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException("Event not found with ID: " + eventId, HttpStatus.NOT_FOUND));

        if (!event.getOrganization().getId().equals(organization.getId())) {
            throw new ApiException("Access Denied: You cannot modify an event belonging to another organization", HttpStatus.FORBIDDEN);
        }

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventType(request.getEventType());
        event.setEventDateTime(request.getEventDateTime());
        event.setUpdatedAt(LocalDateTime.now());

        eventRepository.save(event);
        log.info("Admin '{}' updated event ID {}", adminEmail, event.getId());

        return EventResponseDto.fromEntity(event);
    }

    @Transactional
    public EventResponseDto publishEvent(String adminEmail, Long eventId) {
        Organization organization = getAdminOrganization(adminEmail);

        if (organization.getApprovalStatus() != OrganizationApprovalStatus.APPROVED) {
            throw new ApiException("Organization is not approved yet. Admins cannot publish events until approval.", HttpStatus.FORBIDDEN);
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException("Event not found with ID: " + eventId, HttpStatus.NOT_FOUND));

        if (!event.getOrganization().getId().equals(organization.getId())) {
            throw new ApiException("Access Denied: You cannot publish an event belonging to another organization", HttpStatus.FORBIDDEN);
        }

        if (event.getStatus() == EventStatus.PUBLISHED) {
            throw new ApiException("Event is already published", HttpStatus.BAD_REQUEST);
        }

        event.setStatus(EventStatus.PUBLISHED);
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);

        log.info("Admin '{}' published event ID {}. Triggering postcode notification matching...",
                adminEmail, event.getId());

        // Trigger postcode-based notifications
        notificationManagementService.processEventNotifications(event);

        return EventResponseDto.fromEntity(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponseDto> getAdminEvents(String adminEmail) {
        Organization organization = getAdminOrganization(adminEmail);
        return eventRepository.findByOrganizationIdOrderByEventDateTimeDesc(organization.getId())
                .stream()
                .map(EventResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    private Organization getAdminOrganization(String adminEmail) {
        User user = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ApiException("Admin user not found", HttpStatus.UNAUTHORIZED));

        OrganizationAdmin orgAdmin = organizationAdminRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException("Authenticated user is not registered as an administrator for any religious organization", HttpStatus.FORBIDDEN));

        return orgAdmin.getOrganization();
    }
}
