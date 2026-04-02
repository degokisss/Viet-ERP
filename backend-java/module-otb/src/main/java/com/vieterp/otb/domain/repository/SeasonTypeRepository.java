package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.SeasonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonTypeRepository extends JpaRepository<SeasonType, Long>, JpaSpecificationExecutor<SeasonType> {
}
