package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.ImportedRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository("dataRetentionImportedRecordRepository")
public interface ImportedRecordRepository extends JpaRepository<ImportedRecord, Long>, JpaSpecificationExecutor<ImportedRecord> {

    void deleteByCreatedAtLessThan(Instant cutoff);
}
