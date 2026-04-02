package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.ImportSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository("dataRetentionImportSessionRepository")
public interface ImportSessionRepository extends JpaRepository<ImportSession, Long>, JpaSpecificationExecutor<ImportSession> {

    void deleteByCreatedAtLessThan(Instant cutoff);
}
