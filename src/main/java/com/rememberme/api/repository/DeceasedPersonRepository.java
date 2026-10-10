package com.rememberme.api.repository;

import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.RememberMe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeceasedPersonRepository extends JpaRepository<DeceasedPerson, Long> {
    List<DeceasedPerson> findByFullNameContainingIgnoreCase(String fullName);
    Optional<DeceasedPerson> findFirstByGraveId(Long graveId);
    List<DeceasedPerson> findByGraveId(Long graveId);

    @Query("SELECT DISTINCT d FROM DeceasedPerson d " +
           "LEFT JOIN d.grave g " +
           "LEFT JOIN g.rememberMe r " +
           "WHERE LOWER(d.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "   OR (r IS NOT NULL AND (LOWER(r.city) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "                          OR LOWER(r.country) LIKE LOWER(CONCAT('%', :query, '%'))))")
    List<DeceasedPerson> searchByNameOrLocationAdmin(@Param("query") String query);

    default List<DeceasedPerson> searchByNameOrLocationUser(String query, Long userId) {
        return searchByNameOrLocationUser(query, userId, RememberMe.ApprovalStatus.APPROVED);
    }

    @Query("SELECT DISTINCT d FROM DeceasedPerson d " +
           "LEFT JOIN d.grave g " +
           "LEFT JOIN g.rememberMe r " +
           "WHERE LOWER(d.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "   OR (r IS NOT NULL AND " +
           "       (r.status = :approvedStatus OR (:userId IS NOT NULL AND r.managedBy.id = :userId)) AND " +
           "       (LOWER(r.city) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "        OR LOWER(r.country) LIKE LOWER(CONCAT('%', :query, '%'))))")
    List<DeceasedPerson> searchByNameOrLocationUser(@Param("query") String query,
                                                    @Param("userId") Long userId,
                                                    @Param("approvedStatus") RememberMe.ApprovalStatus approvedStatus);

    @Query("SELECT DISTINCT d FROM DeceasedPerson d " +
           "LEFT JOIN d.grave g " +
           "LEFT JOIN g.rememberMe r " +
           "WHERE (:city IS NULL OR (r IS NOT NULL AND LOWER(r.city) LIKE LOWER(CONCAT('%', :city, '%')))) " +
           "  AND (:country IS NULL OR (r IS NOT NULL AND LOWER(r.country) LIKE LOWER(CONCAT('%', :country, '%'))))")
    List<DeceasedPerson> searchByCityAndCountryAdmin(@Param("city") String city,
                                                     @Param("country") String country);

    default List<DeceasedPerson> searchByCityAndCountryUser(String city, String country, Long userId) {
        return searchByCityAndCountryUser(city, country, userId, RememberMe.ApprovalStatus.APPROVED);
    }

    @Query("SELECT DISTINCT d FROM DeceasedPerson d " +
           "LEFT JOIN d.grave g " +
           "LEFT JOIN g.rememberMe r " +
           "WHERE r IS NOT NULL AND " +
           "      (r.status = :approvedStatus OR (:userId IS NOT NULL AND r.managedBy.id = :userId)) AND " +
           "      (:city IS NULL OR LOWER(r.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "      (:country IS NULL OR LOWER(r.country) LIKE LOWER(CONCAT('%', :country, '%')))")
    List<DeceasedPerson> searchByCityAndCountryUser(@Param("city") String city,
                                                    @Param("country") String country,
                                                    @Param("userId") Long userId,
                                                    @Param("approvedStatus") RememberMe.ApprovalStatus approvedStatus);
}

