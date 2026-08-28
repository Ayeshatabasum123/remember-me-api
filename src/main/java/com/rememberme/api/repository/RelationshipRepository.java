package com.rememberme.api.repository;

import com.rememberme.api.entity.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RelationshipRepository extends JpaRepository<Relationship, Long> {
    boolean existsByDeceasedPersonId(Long deceasedPersonId);
}
