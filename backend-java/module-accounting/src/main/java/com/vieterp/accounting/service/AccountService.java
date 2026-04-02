package com.vieterp.accounting.service;

import com.vieterp.accounting.domain.Account;
import com.vieterp.accounting.event.AccountEventPublisher;
import com.vieterp.accounting.exception.AccountNotFoundException;
import com.vieterp.accounting.repository.AccountRepository;
import com.vieterp.accounting.service.dto.AccountResponse;
import com.vieterp.accounting.service.dto.CreateAccountRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountEventPublisher eventPublisher;

    @Transactional
    public AccountResponse create(CreateAccountRequest req) {
        Account account = Account.builder()
            .accountNumber(req.accountNumber())
            .name(req.name())
            .nameEn(req.nameEn())
            .description(req.description())
            .accountType(req.accountType())
            .accountGroup(req.accountGroup())
            .normalBalance(req.normalBalance())
            .parentId(req.parentId())
            .level(req.level() != null ? req.level() : 1)
            .isActive(req.isActive() != null ? req.isActive() : true)
            .isSystemAccount(req.isSystemAccount() != null ? req.isSystemAccount() : false)
            .isBankAccount(req.isBankAccount() != null ? req.isBankAccount() : false)
            .currency(req.currency() != null ? req.currency() : "VND")
            .vasCode(req.vasCode())
            .ifrsCode(req.ifrsCode())
            .tt200Code(req.tt200Code())
            .tt133Code(req.tt133Code())
            .openingBalance(req.openingBalance() != null ? req.openingBalance() : java.math.BigDecimal.ZERO)
            .currentBalance(req.currentBalance() != null ? req.currentBalance() : java.math.BigDecimal.ZERO)
            .tenantId(req.tenantId())
            .createdBy(req.createdBy())
            .build();

        Account saved = accountRepository.save(account);
        eventPublisher.publishCreated(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getById(UUID id) {
        Account account = accountRepository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException(id));
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAll() {
        return accountRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listByTenantId(String tenantId) {
        return accountRepository.findByTenantId(tenantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public AccountResponse update(UUID id, CreateAccountRequest req) {
        Account account = accountRepository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException(id));

        account.setAccountNumber(req.accountNumber());
        account.setName(req.name());
        account.setNameEn(req.nameEn());
        account.setDescription(req.description());
        account.setAccountType(req.accountType());
        account.setAccountGroup(req.accountGroup());
        account.setNormalBalance(req.normalBalance());
        if (req.parentId() != null) account.setParentId(req.parentId());
        if (req.level() != null) account.setLevel(req.level());
        if (req.isActive() != null) account.setIsActive(req.isActive());
        if (req.isSystemAccount() != null) account.setIsSystemAccount(req.isSystemAccount());
        if (req.isBankAccount() != null) account.setIsBankAccount(req.isBankAccount());
        if (req.currency() != null) account.setCurrency(req.currency());
        account.setVasCode(req.vasCode());
        account.setIfrsCode(req.ifrsCode());
        account.setTt200Code(req.tt200Code());
        account.setTt133Code(req.tt133Code());
        if (req.openingBalance() != null) account.setOpeningBalance(req.openingBalance());
        if (req.currentBalance() != null) account.setCurrentBalance(req.currentBalance());

        Account saved = accountRepository.save(account);
        eventPublisher.publishUpdated(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!accountRepository.existsById(id)) {
            throw new AccountNotFoundException(id);
        }
        accountRepository.deleteById(id);
        eventPublisher.publishDeleted(id);
    }

    private AccountResponse toResponse(Account a) {
        return new AccountResponse(
            a.getId(), a.getAccountNumber(), a.getName(), a.getNameEn(), a.getDescription(),
            a.getAccountType(), a.getAccountGroup(), a.getNormalBalance(), a.getParentId(),
            a.getLevel(), a.getIsActive(), a.getIsSystemAccount(), a.getIsBankAccount(),
            a.getCurrency(), a.getVasCode(), a.getIfrsCode(), a.getTt200Code(), a.getTt133Code(),
            a.getOpeningBalance(), a.getCurrentBalance(), a.getTenantId(), a.getCreatedBy(),
            a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}
