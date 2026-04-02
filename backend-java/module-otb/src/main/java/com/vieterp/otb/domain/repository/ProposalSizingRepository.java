package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.ProposalSizing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProposalSizingRepository extends JpaRepository<ProposalSizing, Long>, JpaSpecificationExecutor<ProposalSizing> {

    List<ProposalSizing> findByProposalSizingHeaderId(Long proposalSizingHeaderId);

    void deleteByProposalSizingHeaderId(Long proposalSizingHeaderId);
}
