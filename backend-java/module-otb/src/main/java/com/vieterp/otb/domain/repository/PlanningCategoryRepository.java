package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.PlanningCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanningCategoryRepository extends JpaRepository<PlanningCategory, Long> {

    List<PlanningCategory> findByPlanningHeaderId(Long planningHeaderId);

    @Modifying
    @Query("DELETE FROM PlanningCategory pc WHERE pc.planningHeaderId = :headerId")
    void deleteByPlanningHeaderId(@Param("headerId") Long headerId);
}
