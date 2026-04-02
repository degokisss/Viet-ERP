package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.BudgetAllocate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetAllocateRepository extends JpaRepository<BudgetAllocate, Long>, JpaSpecificationExecutor<BudgetAllocate> {

    void deleteByAllocateHeaderId(Long allocateHeaderId);
}
