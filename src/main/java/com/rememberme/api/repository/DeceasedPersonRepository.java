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
           "WHERE LOWER(d.fullName) LIKE :pattern " +
           "   OR (r IS NOT NULL AND (LOWER(r.city) LIKE :pattern " +
           "                          OR LOWER(r.country) LIKE :pattern))")
    List<DeceasedPerson> searchByNameOrLocationAdminPattern(@Param("pattern") String pattern);

    default List<DeceasedPerson> searchByNameOrLocationAdmin(String query) {
        if (query == null || query.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return searchByNameOrLocationAdminPattern("%" + query.trim().toLowerCase() + "%");
    }

    @Query("SELECT DISTINCT d FROM DeceasedPerson d " +
           "LEFT JOIN d.grave g " +
           "LEFT JOIN g.rememberMe r " +
           "WHERE LOWER(d.fullName) LIKE :pattern " +
           "   OR (r IS NOT NULL AND " +
           "       (r.status = :approvedStatus OR (:userId IS NOT NULL AND r.managedBy.id = :userId)) AND " +
           "       (LOWER(r.city) LIKE :pattern " +
           "        OR LOWER(r.country) LIKE :pattern))")
    List<DeceasedPerson> searchByNameOrLocationUserPattern(@Param("pattern") String pattern,
                                                           @Param("userId") Long userId,
                                                           @Param("approvedStatus") RememberMe.ApprovalStatus approvedStatus);

    default List<DeceasedPerson> searchByNameOrLocationUser(String query, Long userId) {
        return searchByNameOrLocationUser(query, userId, RememberMe.ApprovalStatus.APPROVED);
    }

    default List<DeceasedPerson> searchByNameOrLocationUser(String query, Long userId, RememberMe.ApprovalStatus approvedStatus) {
        if (query == null || query.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return searchByNameOrLocationUserPattern("%" + query.trim().toLowerCase() + "%", userId, approvedStatus);
    }

    @Query("SELECT DISTINCT d FROM DeceasedPerson d " +
           "LEFT JOIN d.grave g " +
           "LEFT JOIN g.rememberMe r " +
           "WHERE r IS NOT NULL AND LOWER(r.city) LIKE :cityPattern")
    List<DeceasedPerson> searchByCityAdmin(@Param("cityPattern") String cityPattern);

    @Query("SELECT DISTINCT d FROM DeceasedPerson d " +
           "LEFT JOIN d.grave g " +
           "LEFT JOIN g.rememberMe r " +
           "WHERE r IS NOT NULL AND LOWER(r.country) LIKE :countryPattern")
    List<DeceasedPerson> searchByCountryAdmin(@Param("countryPattern") String countryPattern);

    @Query("SELECT DISTINCT d FROM DeceasedPerson d " +
           "LEFT JOIN d.grave g " +
           "LEFT JOIN g.rememberMe r " +
           "WHERE r IS NOT NULL AND LOWER(r.city) LIKE :cityPattern AND LOWER(r.country) LIKE :countryPattern")
    List<DeceasedPerson> searchByBothCityAndCountryAdmin(@Param("cityPattern") String cityPattern,
                                                         @Param("countryPattern") String countryPattern);

    default List<DeceasedPerson> searchByCityAndCountryAdmin(String city, String country) {
        if (city != null && country != null) {
            return searchByBothCityAndCountryAdmin("%" + city.toLowerCase() + "%", "%" + country.toLowerCase() + "%");
        } else if (city != null) {
            return searchByCityAdmin("%" + city.toLowerCase() + "%");
        } else if (country != null) {
            return searchByCountryAdmin("%" + country.toLowerCase() + "%");
        }
        return java.util.Collections.emptyList();
    }

    @Query("SELECT DISTINCT d FROM DeceasedPerson d " +
           "LEFT JOIN d.grave g " +
           "LEFT JOIN g.rememberMe r " +
           "WHERE r IS NOT NULL AND " +
           "      (r.status = :approvedStatus OR (:userId IS NOT NULL AND r.managedBy.id = :userId)) AND " +
           "      LOWER(r.city) LIKE :cityPattern")
    List<DeceasedPerson> searchByCityUser(@Param("cityPattern") String cityPattern,
                                          @Param("userId") Long userId,
                                          @Param("approvedStatus") RememberMe.ApprovalStatus approvedStatus);

    @Query("SELECT DISTINCT d FROM DeceasedPerson d " +
           "LEFT JOIN d.grave g " +
           "LEFT JOIN g.rememberMe r " +
           "WHERE r IS NOT NULL AND " +
           "      (r.status = :approvedStatus OR (:userId IS NOT NULL AND r.managedBy.id = :userId)) AND " +
           "      LOWER(r.country) LIKE :countryPattern")
    List<DeceasedPerson> searchByCountryUser(@Param("countryPattern") String countryPattern,
                                             @Param("userId") Long userId,
                                             @Param("approvedStatus") RememberMe.ApprovalStatus approvedStatus);

    @Query("SELECT DISTINCT d FROM DeceasedPerson d " +
           "LEFT JOIN d.grave g " +
           "LEFT JOIN g.rememberMe r " +
           "WHERE r IS NOT NULL AND " +
           "      (r.status = :approvedStatus OR (:userId IS NOT NULL AND r.managedBy.id = :userId)) AND " +
           "      LOWER(r.city) LIKE :cityPattern AND LOWER(r.country) LIKE :countryPattern")
    List<DeceasedPerson> searchByBothCityAndCountryUser(@Param("cityPattern") String cityPattern,
                                                        @Param("countryPattern") String countryPattern,
                                                        @Param("userId") Long userId,
                                                        @Param("approvedStatus") RememberMe.ApprovalStatus approvedStatus);

    default List<DeceasedPerson> searchByCityAndCountryUser(String city, String country, Long userId) {
        return searchByCityAndCountryUser(city, country, userId, RememberMe.ApprovalStatus.APPROVED);
    }

    default List<DeceasedPerson> searchByCityAndCountryUser(String city, String country, Long userId, RememberMe.ApprovalStatus approvedStatus) {
        if (city != null && country != null) {
            return searchByBothCityAndCountryUser("%" + city.toLowerCase() + "%", "%" + country.toLowerCase() + "%", userId, approvedStatus);
        } else if (city != null) {
            return searchByCityUser("%" + city.toLowerCase() + "%", userId, approvedStatus);
        } else if (country != null) {
            return searchByCountryUser("%" + country.toLowerCase() + "%", userId, approvedStatus);
        }
        return java.util.Collections.emptyList();
    }
}

