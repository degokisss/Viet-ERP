package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.ApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long>, JpaSpecificationExecutor<ApprovalWorkflow> {

    Optional<ApprovalWorkflow> findById(Long id);

    List<ApprovalWorkflow> findByGroupBrandId(Long groupBrandId);
}
