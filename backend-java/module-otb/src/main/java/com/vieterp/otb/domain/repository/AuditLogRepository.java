package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByCreatedAtGreaterThanEqualAndActionIn(Instant since, List<String> actions);

    void deleteByCreatedAtLessThan(Instant cutoff);
}
