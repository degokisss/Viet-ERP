package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.SKUAllocate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SKUAllocateRepository extends JpaRepository<SKUAllocate, Long> {

    List<SKUAllocate> findBySkuProposalId(Long skuProposalId);

    void deleteBySkuProposalId(Long skuProposalId);
}
