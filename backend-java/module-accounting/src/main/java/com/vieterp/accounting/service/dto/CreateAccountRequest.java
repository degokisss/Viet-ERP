package com.vieterp.accounting.service.dto;

import com.vieterp.accounting.domain.enums.AccountGroup;
import com.vieterp.accounting.domain.enums.AccountType;
import com.vieterp.accounting.domain.enums.NormalBalance;
import jakarta.validation.constraints.*;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record CreateAccountRequest(
    @NotBlank(message = "Account number is required") @Size(max = 50) String accountNumber,
    @NotBlank(message = "Name is required") @Size(max = 255) String name,
    @Size(max = 255) String nameEn,
    String description,
    @NotNull(message = "Account type is required") AccountType accountType,
    @NotNull(message = "Account group is required") AccountGroup accountGroup,
    @NotNull(message = "Normal balance is required") NormalBalance normalBalance,
    String parentId,
    @Min(value = 1, message = "Level must be at least 1") Integer level,
    Boolean isActive,
    Boolean isSystemAccount,
    Boolean isBankAccount,
    @Size(max = 10) String currency,
    @Size(max = 50) String vasCode,
    @Size(max = 50) String ifrsCode,
    @Size(max = 50) String tt200Code,
    @Size(max = 50) String tt133Code,
    @DecimalMin(value = "0.0", message = "Opening balance must be non-negative") BigDecimal openingBalance,
    @DecimalMin(value = "0.0", message = "Current balance must be non-negative") BigDecimal currentBalance,
    @NotBlank(message = "Tenant ID is required") String tenantId,
    String createdBy
) {}
