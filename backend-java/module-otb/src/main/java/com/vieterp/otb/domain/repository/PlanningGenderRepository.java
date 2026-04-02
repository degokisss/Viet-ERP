package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.PlanningGender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanningGenderRepository extends JpaRepository<PlanningGender, Long> {

    List<PlanningGender> findByPlanningHeaderId(Long planningHeaderId);

    @Modifying
    @Query("DELETE FROM PlanningGender pg WHERE pg.planningHeaderId = :headerId")
    void deleteByPlanningHeaderId(@Param("headerId") Long headerId);
}
