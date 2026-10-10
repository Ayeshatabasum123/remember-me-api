package com.rememberme.api.repository;

import com.rememberme.api.entity.DeceasedPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeceasedPersonRepository extends JpaRepository<DeceasedPerson, Long> {
    List<DeceasedPerson> findByFullNameContainingIgnoreCase(String fullName);
    Optional<DeceasedPerson> findFirstByGraveId(Long graveId);
    List<DeceasedPerson> findByGraveId(Long graveId);
}
