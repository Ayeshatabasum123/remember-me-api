package com.rememberme.api.repository;

import com.rememberme.api.entity.DeceasedPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeceasedPersonRepository extends JpaRepository<DeceasedPerson, Long> {
    List<DeceasedPerson> findByFullNameContainingIgnoreCase(String fullName);
}
