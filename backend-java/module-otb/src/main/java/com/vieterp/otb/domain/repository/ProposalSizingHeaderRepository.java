package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.ProposalSizingHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProposalSizingHeaderRepository extends JpaRepository<ProposalSizingHeader, Long> {

    List<ProposalSizingHeader> findBySkuProposalHeaderId(Long skuProposalHeaderId);

    long countBySkuProposalHeaderId(Long skuProposalHeaderId);

    void deleteBySkuProposalHeaderId(Long skuProposalHeaderId);
}
