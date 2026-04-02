package com.vieterp.otb.domain.repository;

import com.vieterp.otb.domain.GroupBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupBrandRepository extends JpaRepository<GroupBrand, Long> {
}
