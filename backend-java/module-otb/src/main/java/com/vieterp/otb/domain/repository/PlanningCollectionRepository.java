package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.PlanningCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanningCollectionRepository extends JpaRepository<PlanningCollection, Long> {

    List<PlanningCollection> findByPlanningHeaderId(Long planningHeaderId);

    @Modifying
    @Query("DELETE FROM PlanningCollection pc WHERE pc.planningHeaderId = :headerId")
    void deleteByPlanningHeaderId(@Param("headerId") Long headerId);
}
