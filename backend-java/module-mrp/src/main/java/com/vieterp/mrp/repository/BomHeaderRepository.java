package com.vieterp.mrp.repository;

import com.vieterp.mrp.domain.BomHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BomHeaderRepository extends JpaRepository<BomHeader, UUID> {
    List<BomHeader> findByTenantId(String tenantId);
    Optional<BomHeader> findByBomNumber(String bomNumber);
    Optional<BomHeader> findByBomNumberAndTenantId(String bomNumber, String tenantId);
    List<BomHeader> findByPartId(String partId);
}
