package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.SKUProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SKUProposalRepository extends JpaRepository<SKUProposal, Long>, JpaSpecificationExecutor<SKUProposal> {

    List<SKUProposal> findBySkuProposalHeaderId(Long skuProposalHeaderId);

    @Query("SELECT p FROM SKUProposal p WHERE p.id = :id")
    Optional<SKUProposal> findByIdSimple(@Param("id") Long id);

    void deleteBySkuProposalHeaderId(Long skuProposalHeaderId);
}
