package com.vieterp.otb.imports.repository;

import com.vieterp.otb.domain.ImportedRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportedRecordRepository extends JpaRepository<ImportedRecord, Long>, JpaSpecificationExecutor<ImportedRecord> {

    void deleteBySessionId(Long sessionId);

    List<ImportedRecord> findBySessionId(Long sessionId);
}
