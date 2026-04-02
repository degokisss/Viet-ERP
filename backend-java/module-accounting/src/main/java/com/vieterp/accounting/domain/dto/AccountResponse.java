package com.vieterp.accounting.domain.dto;

import com.vieterp.accounting.domain.enums.AccountGroup;
import com.vieterp.accounting.domain.enums.AccountType;
import com.vieterp.accounting.domain.enums.NormalBalance;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AccountResponse(
    UUID id,
    String accountNumber,
    String name,
    String nameEn,
    String description,
    AccountType accountType,
    AccountGroup accountGroup,
    NormalBalance normalBalance,
    String parentId,
    Integer level,
    Boolean isActive,
    Boolean isSystemAccount,
    Boolean isBankAccount,
    String currency,
    String vasCode,
    String ifrsCode,
    String tt200Code,
    String tt133Code,
    BigDecimal openingBalance,
    BigDecimal currentBalance,
    String tenantId,
    String createdBy,
    Instant createdAt,
    Instant updatedAt
) {}
