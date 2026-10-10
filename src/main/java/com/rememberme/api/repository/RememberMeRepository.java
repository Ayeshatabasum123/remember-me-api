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
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.country) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<RememberMe> searchAllFieldsAdmin(@Param("query") String query);

    default List<RememberMe> searchAllFieldsUser(String query, Long userId) {
        return searchAllFieldsUser(query, userId, RememberMe.ApprovalStatus.APPROVED);
    }

    @Query("SELECT r FROM RememberMe r WHERE " +
           "(r.status = :approvedStatus OR (:userId IS NOT NULL AND r.managedBy.id = :userId)) AND " +
           "(LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(r.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(r.country) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<RememberMe> searchAllFieldsUser(@Param("query") String query,
                                         @Param("userId") Long userId,
                                         @Param("approvedStatus") RememberMe.ApprovalStatus approvedStatus);

    @Query("SELECT r FROM RememberMe r WHERE " +
           "(:city IS NULL OR LOWER(r.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:country IS NULL OR LOWER(r.country) LIKE LOWER(CONCAT('%', :country, '%')))")
    List<RememberMe> searchByCityAndCountryAdmin(@Param("city") String city,
                                                 @Param("country") String country);

    default List<RememberMe> searchByCityAndCountryUser(String city, String country, Long userId) {
        return searchByCityAndCountryUser(city, country, userId, RememberMe.ApprovalStatus.APPROVED);
    }

    @Query("SELECT r FROM RememberMe r WHERE " +
           "(r.status = :approvedStatus OR (:userId IS NOT NULL AND r.managedBy.id = :userId)) AND " +
           "(:city IS NULL OR LOWER(r.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:country IS NULL OR LOWER(r.country) LIKE LOWER(CONCAT('%', :country, '%')))")
    List<RememberMe> searchByCityAndCountryUser(@Param("city") String city,
                                                @Param("country") String country,
                                                @Param("userId") Long userId,
                                                @Param("approvedStatus") RememberMe.ApprovalStatus approvedStatus);
}


