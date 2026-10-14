package com.rememberme.api.repository;

import com.rememberme.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByPostcodeNormalized(String postcodeNormalized);

    @Query("SELECT u FROM User u WHERE " +
           "((u.postcodeNormalized IS NOT NULL AND u.postcodeNormalized = :postcodeNorm) OR " +
           "(u.postcode IS NOT NULL AND UPPER(REPLACE(u.postcode, ' ', '')) = :postcodeNorm))")
    List<User> findMatchingResidentsByPostcode(@Param("postcodeNorm") String postcodeNorm);
}
