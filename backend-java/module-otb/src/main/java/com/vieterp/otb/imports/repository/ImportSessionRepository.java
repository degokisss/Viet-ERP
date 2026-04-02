package com.vieterp.otb.imports.repository;

import com.vieterp.otb.domain.ImportSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImportSessionRepository extends JpaRepository<ImportSession, Long>, JpaSpecificationExecutor<ImportSession> {

    Optional<ImportSession> findByFileName(String fileName);
}
