package com.rememberme.api.repository;

import com.rememberme.api.entity.RememberMe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RememberMeRepository extends JpaRepository<RememberMe, Long> {
    List<RememberMe> findByNameContainingIgnoreCase(String name);
    Optional<RememberMe> findFirstByNameIgnoreCase(String name);
    List<RememberMe> findByStatus(RememberMe.ApprovalStatus status);
    List<RememberMe> findByIsFamousTrueOrIsHistoricalTrue();
    boolean existsByNameIgnoreCaseAndLatitudeAndLongitude(String name, Double latitude, Double longitude);

    @Query("SELECT r FROM RememberMe r WHERE " +
           "LOWER(r.name) LIKE :pattern OR " +
           "LOWER(r.city) LIKE :pattern OR " +
           "LOWER(r.country) LIKE :pattern")
    List<RememberMe> searchAllFieldsAdminPattern(@Param("pattern") String pattern);

    default List<RememberMe> searchAllFieldsAdmin(String query) {
        if (query == null || query.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return searchAllFieldsAdminPattern("%" + query.trim().toLowerCase() + "%");
    }

    @Query("SELECT r FROM RememberMe r WHERE " +
           "(r.status = :approvedStatus OR (:userId IS NOT NULL AND r.managedBy.id = :userId)) AND " +
           "(LOWER(r.name) LIKE :pattern OR " +
           " LOWER(r.city) LIKE :pattern OR " +
           " LOWER(r.country) LIKE :pattern)")
    List<RememberMe> searchAllFieldsUserPattern(@Param("pattern") String pattern,
                                                @Param("userId") Long userId,
                                                @Param("approvedStatus") RememberMe.ApprovalStatus approvedStatus);

    default List<RememberMe> searchAllFieldsUser(String query, Long userId) {
        return searchAllFieldsUser(query, userId, RememberMe.ApprovalStatus.APPROVED);
    }

    default List<RememberMe> searchAllFieldsUser(String query, Long userId, RememberMe.ApprovalStatus approvedStatus) {
        if (query == null || query.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return searchAllFieldsUserPattern("%" + query.trim().toLowerCase() + "%", userId, approvedStatus);
    }

    @Query("SELECT r FROM RememberMe r WHERE LOWER(r.city) LIKE :cityPattern")
    List<RememberMe> searchByCityAdmin(@Param("cityPattern") String cityPattern);

    @Query("SELECT r FROM RememberMe r WHERE LOWER(r.country) LIKE :countryPattern")
    List<RememberMe> searchByCountryAdmin(@Param("countryPattern") String countryPattern);

    @Query("SELECT r FROM RememberMe r WHERE LOWER(r.city) LIKE :cityPattern AND LOWER(r.country) LIKE :countryPattern")
    List<RememberMe> searchByBothCityAndCountryAdmin(@Param("cityPattern") String cityPattern,
                                                     @Param("countryPattern") String countryPattern);

    default List<RememberMe> searchByCityAndCountryAdmin(String city, String country) {
        if (city != null && country != null) {
            return searchByBothCityAndCountryAdmin("%" + city.toLowerCase() + "%", "%" + country.toLowerCase() + "%");
        } else if (city != null) {
            return searchByCityAdmin("%" + city.toLowerCase() + "%");
        } else if (country != null) {
            return searchByCountryAdmin("%" + country.toLowerCase() + "%");
        }
        return java.util.Collections.emptyList();
    }

    @Query("SELECT r FROM RememberMe r WHERE " +
           "(r.status = :approvedStatus OR (:userId IS NOT NULL AND r.managedBy.id = :userId)) AND " +
           "LOWER(r.city) LIKE :cityPattern")
    List<RememberMe> searchByCityUser(@Param("cityPattern") String cityPattern,
                                      @Param("userId") Long userId,
                                      @Param("approvedStatus") RememberMe.ApprovalStatus approvedStatus);

    @Query("SELECT r FROM RememberMe r WHERE " +
           "(r.status = :approvedStatus OR (:userId IS NOT NULL AND r.managedBy.id = :userId)) AND " +
           "LOWER(r.country) LIKE :countryPattern")
    List<RememberMe> searchByCountryUser(@Param("countryPattern") String countryPattern,
                                         @Param("userId") Long userId,
                                         @Param("approvedStatus") RememberMe.ApprovalStatus approvedStatus);

    @Query("SELECT r FROM RememberMe r WHERE " +
           "(r.status = :approvedStatus OR (:userId IS NOT NULL AND r.managedBy.id = :userId)) AND " +
           "LOWER(r.city) LIKE :cityPattern AND LOWER(r.country) LIKE :countryPattern")
    List<RememberMe> searchByBothCityAndCountryUser(@Param("cityPattern") String cityPattern,
                                                    @Param("countryPattern") String countryPattern,
                                                    @Param("userId") Long userId,
                                                    @Param("approvedStatus") RememberMe.ApprovalStatus approvedStatus);

    default List<RememberMe> searchByCityAndCountryUser(String city, String country, Long userId) {
        return searchByCityAndCountryUser(city, country, userId, RememberMe.ApprovalStatus.APPROVED);
    }

    default List<RememberMe> searchByCityAndCountryUser(String city, String country, Long userId, RememberMe.ApprovalStatus approvedStatus) {
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


