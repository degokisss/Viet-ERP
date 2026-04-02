package com.vieterp.accounting.service;

import com.vieterp.accounting.domain.Account;
import com.vieterp.accounting.domain.enums.AccountGroup;
import com.vieterp.accounting.domain.enums.AccountType;
import com.vieterp.accounting.domain.enums.NormalBalance;
import com.vieterp.accounting.exception.AccountNotFoundException;
import com.vieterp.accounting.repository.AccountRepository;
import com.vieterp.accounting.domain.dto.AccountResponse;
import com.vieterp.accounting.domain.dto.CreateAccountRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;

    private AccountEventPublisherTestDouble eventPublisher = new AccountEventPublisherTestDouble();

    private AccountService accountService;

    private Account testAccount;
    private UUID testId;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, eventPublisher);

        testId = UUID.randomUUID();
        testAccount = Account.builder()
            .id(testId)
            .accountNumber("1111")
            .name("Cash")
            .nameEn("Cash")
            .description("Cash account")
            .accountType(AccountType.ASSET)
            .accountGroup(AccountGroup.GROUP_1)
            .normalBalance(NormalBalance.DEBIT)
            .level(1)
            .isActive(true)
            .isSystemAccount(false)
            .isBankAccount(false)
            .currency("VND")
            .openingBalance(BigDecimal.ZERO)
            .currentBalance(BigDecimal.ZERO)
            .tenantId("tenant-123")
            .createdBy("user-123")
            .build();
    }

    @Test
    void create_savesAccountAndPublishesEvent() {
        CreateAccountRequest req = new CreateAccountRequest(
            "1111", "Cash", "Cash", "Cash account",
            AccountType.ASSET, AccountGroup.GROUP_1, NormalBalance.DEBIT,
            null, 1, true, false, false, "VND",
            "VAS111", "CASH", "TT200-111", "TT133-111",
            BigDecimal.ZERO, BigDecimal.ZERO, "tenant-123", "user-123"
        );

        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        AccountResponse resp = accountService.create(req);

        assertNotNull(resp);
        assertEquals(testId, resp.id());
        assertEquals("Cash", resp.name());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(accountRepository.findById(testId)).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> accountService.getById(testId));
    }

    @Test
    void listAll_returnsAllAccounts() {
        when(accountRepository.findAll()).thenReturn(List.of(testAccount));
        List<AccountResponse> result = accountService.listAll();
        assertEquals(1, result.size());
        assertEquals(testId, result.get(0).id());
    }

    @Test
    void delete_deletesWhenExists() {
        when(accountRepository.existsById(testId)).thenReturn(true);
        doNothing().when(accountRepository).deleteById(testId);
        accountService.delete(testId);
        verify(accountRepository).deleteById(testId);
    }

    @Test
    void update_updatesAccountAndPublishesEvent() {
        CreateAccountRequest req = new CreateAccountRequest(
            "1112", "Updated Cash", "Updated Cash", "Updated description",
            AccountType.ASSET, AccountGroup.GROUP_1, NormalBalance.DEBIT,
            null, 2, true, false, true, "USD",
            "VAS112", "CASH2", "TT200-112", "TT133-112",
            BigDecimal.valueOf(1000), BigDecimal.valueOf(500), "tenant-123", "user-456"
        );

        when(accountRepository.findById(testId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountResponse resp = accountService.update(testId, req);

        assertNotNull(resp);
        verify(accountRepository).save(any(Account.class));
        assertTrue(eventPublisher.wasPublishUpdatedCalled());
    }
}
