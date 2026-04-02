package com.vieterp.tpm.repository;

import com.vieterp.tpm.domain.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    List<Claim> findByTenantId(String tenantId);
    Optional<Claim> findByClaimNumber(String claimNumber);
    Optional<Claim> findByClaimNumberAndTenantId(String claimNumber, String tenantId);
}
