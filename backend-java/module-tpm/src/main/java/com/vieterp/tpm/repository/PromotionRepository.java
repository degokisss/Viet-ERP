package com.vieterp.tpm.repository;

import com.vieterp.tpm.domain.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {
    List<Promotion> findByTenantId(String tenantId);
    Optional<Promotion> findByNameAndTenantId(String name, String tenantId);
}
