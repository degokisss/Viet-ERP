package com.vieterp.accounting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vieterp.accounting.BaseIntegrationTest;
import com.vieterp.accounting.config.TestSecurityConfig;
import com.vieterp.accounting.config.TestAccountEventPublisher;
import com.vieterp.accounting.domain.dto.AccountResponse;
import com.vieterp.accounting.domain.dto.CreateAccountRequest;
import com.vieterp.accounting.domain.Account;
import com.vieterp.accounting.domain.enums.AccountGroup;
import com.vieterp.accounting.domain.enums.AccountType;
import com.vieterp.accounting.domain.enums.NormalBalance;
import com.vieterp.accounting.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class, TestAccountEventPublisher.class})
class AccountControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String tenantId = "test-tenant";

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    void createAccount_returnsCreatedAndPersisted() throws Exception {
        CreateAccountRequest req = new CreateAccountRequest(
            "1111", "Cash Account", "Cash", "Main cash account",
            AccountType.ASSET, AccountGroup.GROUP_1, NormalBalance.DEBIT,
            null, 1, true, false, false, "VND",
            "VAS111", "CASH", "TT200-111", "TT133-111",
            BigDecimal.ZERO, BigDecimal.ZERO, tenantId, "user-123"
        );

        mockMvc.perform(post("/api/v1/accounting/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountNumber").value("1111"))
            .andExpect(jsonPath("$.name").value("Cash Account"))
            .andExpect(jsonPath("$.accountType").value("ASSET"))
            .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void getById_returnsAccount() throws Exception {
        Account saved = createTestAccount("2222", "Bank Account", AccountType.ASSET);

        mockMvc.perform(get("/api/v1/accounting/accounts/" + saved.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountNumber").value("2222"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/accounting/accounts/" + UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }

    @Test
    void listAll_returnsAllAccounts() throws Exception {
        createTestAccount("3333", "Asset 1", AccountType.ASSET);
        createTestAccount("3334", "Asset 2", AccountType.ASSET);

        mockMvc.perform(get("/api/v1/accounting/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void listByTenantId_returnsOnlyTenantAccounts() throws Exception {
        createTestAccount("4441", "Tenant Asset", AccountType.ASSET);
        createTestAccount("4442", "Tenant Liability", AccountType.LIABILITY);

        mockMvc.perform(get("/api/v1/accounting/accounts/tenant/" + tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void updateAccount_returnsUpdatedAccount() throws Exception {
        Account saved = createTestAccount("5551", "Original Name", AccountType.ASSET);

        CreateAccountRequest updateReq = new CreateAccountRequest(
            "5551", "Updated Name", "Updated EN", "Updated description",
            AccountType.LIABILITY, AccountGroup.GROUP_2, NormalBalance.CREDIT,
            null, 2, false, false, true, "USD",
            "VAS551", "UPD", "TT200-551", "TT133-551",
            BigDecimal.valueOf(1000), BigDecimal.valueOf(500), tenantId, "user-456"
        );

        mockMvc.perform(put("/api/v1/accounting/accounts/" + saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Name"))
            .andExpect(jsonPath("$.accountType").value("LIABILITY"));
    }

    @Test
    void deleteAccount_returnsNoContent() throws Exception {
        Account saved = createTestAccount("6661", "To Delete", AccountType.ASSET);

        mockMvc.perform(delete("/api/v1/accounting/accounts/" + saved.getId()))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteAccount_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/accounting/accounts/" + UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }

    private Account createTestAccount(String number, String name, AccountType type) {
        Account account = Account.builder()
            .accountNumber(number)
            .name(name)
            .nameEn(name + " EN")
            .description("Test account")
            .accountType(type)
            .accountGroup(AccountGroup.GROUP_1)
            .normalBalance(NormalBalance.DEBIT)
            .level(1)
            .isActive(true)
            .isSystemAccount(false)
            .isBankAccount(false)
            .currency("VND")
            .openingBalance(BigDecimal.ZERO)
            .currentBalance(BigDecimal.ZERO)
            .tenantId(tenantId)
            .createdBy("test-user")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        return accountRepository.save(account);
    }
}
