package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.AllocateHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AllocateHeaderRepository extends JpaRepository<AllocateHeader, Long>, JpaSpecificationExecutor<AllocateHeader> {

    @Query("SELECT ah FROM AllocateHeader ah " +
           "LEFT JOIN FETCH ah.budgetAllocates " +
           "LEFT JOIN FETCH ah.brand b " +
           "LEFT JOIN FETCH b.groupBrand " +
           "WHERE ah.id = :id AND ah.isSnapshot = false")
    Optional<AllocateHeader> findByIdWithAllocations(@Param("id") Long id);

    @Query("SELECT ah FROM AllocateHeader ah " +
           "LEFT JOIN FETCH ah.budgetAllocates " +
           "WHERE ah.budget.id = :budgetId AND ah.brand.id = :brandId AND ah.isSnapshot = false " +
           "ORDER BY ah.version DESC")
    Optional<AllocateHeader> findLatestByBudgetAndBrand(
        @Param("budgetId") Long budgetId,
        @Param("brandId") Long brandId
    );

    @Query("SELECT ah FROM AllocateHeader ah WHERE ah.budget.id = :budgetId AND ah.brand.id = :brandId AND ah.isSnapshot = false AND ah.id != :excludeId")
    java.util.List<AllocateHeader> findOthersByBudgetAndBrand(
        @Param("budgetId") Long budgetId,
        @Param("brandId") Long brandId,
        @Param("excludeId") Long excludeId
    );

    @Query("SELECT ah FROM AllocateHeader ah WHERE ah.budget.id = :budgetId AND ah.brand.id = :brandId AND ah.isFinalVersion = true AND ah.isSnapshot = false")
    java.util.List<AllocateHeader> findFinalVersionByBudgetAndBrand(
        @Param("budgetId") Long budgetId,
        @Param("brandId") Long brandId
    );
}
