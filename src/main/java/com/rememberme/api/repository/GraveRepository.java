package com.rememberme.api.repository;

import com.rememberme.api.entity.Grave;
import com.rememberme.api.entity.RememberMe;
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

    default List<Grave> searchByCityAndCountryAdmin(String city, String country) {
        if (city != null && country != null) {
            return searchByBothCityAndCountryAdmin("%" + city.toLowerCase() + "%", "%" + country.toLowerCase() + "%");
        } else if (city != null) {
            return searchByCityAdmin("%" + city.toLowerCase() + "%");
        } else if (country != null) {
            return searchByCountryAdmin("%" + country.toLowerCase() + "%");
        }
        return java.util.Collections.emptyList();
    }

    @Query("SELECT g FROM Grave g WHERE LOWER(g.rememberMe.city) LIKE :cityPattern")
    List<Grave> searchByCityAdmin(@Param("cityPattern") String cityPattern);

    @Query("SELECT g FROM Grave g WHERE LOWER(g.rememberMe.country) LIKE :countryPattern")
    List<Grave> searchByCountryAdmin(@Param("countryPattern") String countryPattern);

    @Query("SELECT g FROM Grave g WHERE LOWER(g.rememberMe.city) LIKE :cityPattern AND LOWER(g.rememberMe.country) LIKE :countryPattern")
    List<Grave> searchByBothCityAndCountryAdmin(@Param("cityPattern") String cityPattern,
                                                @Param("countryPattern") String countryPattern);

    default List<Grave> searchByCityAndCountryUser(String city, String country, Long userId) {
        return searchByCityAndCountryUser(city, country, userId, RememberMe.ApprovalStatus.APPROVED);
    }

    default List<Grave> searchByCityAndCountryUser(String city, String country, Long userId, RememberMe.ApprovalStatus approvedStatus) {
        if (city != null && country != null) {
            return searchByBothCityAndCountryUser("%" + city.toLowerCase() + "%", "%" + country.toLowerCase() + "%", userId, approvedStatus);
        } else if (city != null) {
            return searchByCityUser("%" + city.toLowerCase() + "%", userId, approvedStatus);
        } else if (country != null) {
            return searchByCountryUser("%" + country.toLowerCase() + "%", userId, approvedStatus);
        }
        return java.util.Collections.emptyList();
    }

    @Query("SELECT g FROM Grave g WHERE " +
           "(g.rememberMe.status = :approvedStatus OR (:userId IS NOT NULL AND g.rememberMe.managedBy.id = :userId)) AND " +
           "LOWER(g.rememberMe.city) LIKE :cityPattern")
    List<Grave> searchByCityUser(@Param("cityPattern") String cityPattern,
                                 @Param("userId") Long userId,
                                 @Param("approvedStatus") RememberMe.ApprovalStatus approvedStatus);

    @Query("SELECT g FROM Grave g WHERE " +
           "(g.rememberMe.status = :approvedStatus OR (:userId IS NOT NULL AND g.rememberMe.managedBy.id = :userId)) AND " +
           "LOWER(g.rememberMe.country) LIKE :countryPattern")
    List<Grave> searchByCountryUser(@Param("countryPattern") String countryPattern,
                                    @Param("userId") Long userId,
                                    @Param("approvedStatus") RememberMe.ApprovalStatus approvedStatus);

    @Query("SELECT g FROM Grave g WHERE " +
           "(g.rememberMe.status = :approvedStatus OR (:userId IS NOT NULL AND g.rememberMe.managedBy.id = :userId)) AND " +
           "LOWER(g.rememberMe.city) LIKE :cityPattern AND LOWER(g.rememberMe.country) LIKE :countryPattern")
    List<Grave> searchByBothCityAndCountryUser(@Param("cityPattern") String cityPattern,
                                               @Param("countryPattern") String countryPattern,
                                               @Param("userId") Long userId,
                                               @Param("approvedStatus") RememberMe.ApprovalStatus approvedStatus);
}
