package com.vieterp.otb.budget.repository;

import com.vieterp.otb.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long>, JpaSpecificationExecutor<Budget> {

    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.allocateHeaders WHERE b.id = :id")
    Optional<Budget> findByIdWithAllocations(@Param("id") Long id);
}
