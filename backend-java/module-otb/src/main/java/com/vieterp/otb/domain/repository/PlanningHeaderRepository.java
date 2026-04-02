package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.PlanningHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanningHeaderRepository extends JpaRepository<PlanningHeader, Long>, JpaSpecificationExecutor<PlanningHeader> {

    @Query("SELECT ph FROM PlanningHeader ph " +
           "LEFT JOIN FETCH ph.creator " +
           "LEFT JOIN FETCH ph.allocateHeader ah " +
           "LEFT JOIN FETCH ah.brand " +
           "WHERE ph.id = :id")
    Optional<PlanningHeader> findByIdWithHeader(@Param("id") Long id);

    @Query("SELECT ph FROM PlanningHeader ph " +
           "LEFT JOIN FETCH ph.creator " +
           "WHERE ph.allocateHeader.id = :allocateHeaderId AND ph.allocateHeader.isSnapshot = false " +
           "ORDER BY ph.version DESC")
    List<PlanningHeader> findByAllocateHeaderIdOrderByVersionDesc(@Param("allocateHeaderId") Long allocateHeaderId);

    @Query("SELECT ph FROM PlanningHeader ph " +
           "WHERE ph.allocateHeader.brand.id = :brandId AND ph.allocateHeader.isSnapshot = false " +
           "ORDER BY ph.version DESC")
    List<PlanningHeader> findByBrandIdOrderByVersionDesc(@Param("brandId") Long brandId);

    @Query("SELECT ph FROM PlanningHeader ph WHERE ph.allocateHeader.id = :allocateHeaderId AND ph.isFinalVersion = true")
    List<PlanningHeader> findFinalVersionByAllocateHeader(@Param("allocateHeaderId") Long allocateHeaderId);
}
