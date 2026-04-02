package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.SKUProposalHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SKUProposalHeaderRepository extends JpaRepository<SKUProposalHeader, Long>, JpaSpecificationExecutor<SKUProposalHeader> {

    @Query("SELECT sh FROM SKUProposalHeader sh WHERE sh.allocateHeader.id = :allocateHeaderId AND sh.isFinalVersion = true")
    List<SKUProposalHeader> findFinalVersionByAllocateHeader(@Param("allocateHeaderId") Long allocateHeaderId);
}
