package com.vieterp.accounting.repository;

import com.vieterp.accounting.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByTenantId(String tenantId);
    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findByAccountNumberAndTenantId(String accountNumber, String tenantId);
}
