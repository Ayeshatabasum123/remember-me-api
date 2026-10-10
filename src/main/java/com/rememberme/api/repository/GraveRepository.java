package com.rememberme.api.repository;

import com.rememberme.api.entity.Grave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GraveRepository extends JpaRepository<Grave, Long> {
    boolean existsByRememberMeId(Long rememberMeId);
    boolean existsByRememberMeIdAndGraveNumberIgnoreCase(Long rememberMeId, String graveNumber);
    List<Grave> findByRememberMeId(Long rememberMeId);
    Page<Grave> findByRememberMeId(Long rememberMeId, Pageable pageable);

    @Query("SELECT g FROM Grave g WHERE " +
           "(:rememberMeId IS NULL OR g.rememberMe.id = :rememberMeId) AND " +
           "(:search IS NULL OR LOWER(g.graveNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(g.rememberMe.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Grave> searchGraves(@Param("rememberMeId") Long rememberMeId,
                             @Param("search") String search,
                             Pageable pageable);

    @Query("SELECT g FROM Grave g WHERE " +
           "(:city IS NULL OR LOWER(g.rememberMe.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:country IS NULL OR LOWER(g.rememberMe.country) LIKE LOWER(CONCAT('%', :country, '%')))")
    List<Grave> searchByCityAndCountryAdmin(@Param("city") String city,
                                            @Param("country") String country);

    @Query("SELECT g FROM Grave g WHERE " +
           "(g.rememberMe.status = com.rememberme.api.entity.RememberMe.ApprovalStatus.APPROVED " +
           " OR (:userId IS NOT NULL AND g.rememberMe.managedBy.id = :userId)) AND " +
           "(:city IS NULL OR LOWER(g.rememberMe.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:country IS NULL OR LOWER(g.rememberMe.country) LIKE LOWER(CONCAT('%', :country, '%')))")
    List<Grave> searchByCityAndCountryUser(@Param("city") String city,
                                           @Param("country") String country,
                                           @Param("userId") Long userId);
}
