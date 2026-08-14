package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.Relationship;
import com.rememberme.api.repository.RelationshipRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/relationships")
@RequiredArgsConstructor
@Tag(name = "Relationship", description = "Links the logged-in user to a deceased person")
public class RelationshipController {

    private final RelationshipRepository relationshipRepository;

    @PostMapping
    public ApiResponse<Relationship> addRelationship(@RequestBody Relationship relationship) {
        return ApiResponse.success(relationshipRepository.save(relationship));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<Relationship>> getByUser(@PathVariable Long userId) {
        return ApiResponse.success(relationshipRepository.findAll().stream()
                .filter(r -> r.getUser().getId().equals(userId))
                .toList());
    }
}
