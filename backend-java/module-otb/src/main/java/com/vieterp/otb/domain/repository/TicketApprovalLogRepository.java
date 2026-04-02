package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.TicketApprovalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketApprovalLogRepository extends JpaRepository<TicketApprovalLog, Long> {

    List<TicketApprovalLog> findByTicketIdOrderByApprovedAtDesc(Long ticketId);
}
