package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.ApprovalWorkflowLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalWorkflowLevelRepository extends JpaRepository<ApprovalWorkflowLevel, Long> {

    List<ApprovalWorkflowLevel> findByApprovalWorkflowIdOrderByLevelOrder(Long approvalWorkflowId);

    @Query("SELECT MAX(awl.levelOrder) FROM ApprovalWorkflowLevel awl WHERE awl.approvalWorkflowId = :workflowId")
    Integer findMaxLevelOrderByWorkflowId(@Param("workflowId") Long workflowId);
}
