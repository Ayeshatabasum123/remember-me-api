package com.rememberme.api.repository;

import com.rememberme.api.entity.FuneralEvent;
import com.rememberme.api.entity.Religion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FuneralEventRepository extends JpaRepository<FuneralEvent, Long> {

    List<FuneralEvent> findByStatus(FuneralEvent.FuneralStatus status);

    @Query("SELECT f FROM FuneralEvent f WHERE f.status = 'SCHEDULED' AND f.funeralDateTime >= :fromTime")
    List<FuneralEvent> findUpcomingFunerals(@Param("fromTime") LocalDateTime fromTime);

    @Query("SELECT f FROM FuneralEvent f WHERE f.status = 'SCHEDULED' AND f.religion IN :religions")
    List<FuneralEvent> findByReligions(@Param("religions") List<Religion> religions);
}
