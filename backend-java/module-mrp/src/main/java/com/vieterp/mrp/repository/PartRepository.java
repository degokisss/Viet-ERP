package com.vieterp.mrp.repository;

import com.vieterp.mrp.domain.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartRepository extends JpaRepository<Part, UUID> {
    List<Part> findByTenantId(String tenantId);
    Optional<Part> findByPartNumber(String partNumber);
    Optional<Part> findByPartNumberAndTenantId(String partNumber, String tenantId);
}
