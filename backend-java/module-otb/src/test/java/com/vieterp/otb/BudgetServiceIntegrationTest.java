package com.vieterp.otb;

import com.vieterp.otb.budget.BudgetService;
import com.vieterp.otb.budget.domain.dto.*;
import com.vieterp.otb.budget.repository.BudgetRepository;
import com.vieterp.otb.domain.*;
import com.vieterp.otb.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class BudgetServiceIntegrationTest extends BaseIntegrationTest {

    @MockBean
    JwtDecoder jwtDecoder;

    @Autowired
    BudgetService budgetService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BrandRepository brandRepository;

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    SeasonGroupRepository seasonGroupRepository;

    @Autowired
    SeasonRepository seasonRepository;

    @Autowired
    BudgetRepository budgetRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        budgetRepository.deleteAll();
        userRepository.deleteAll();
        brandRepository.deleteAll();
        storeRepository.deleteAll();
        seasonGroupRepository.deleteAll();
        seasonRepository.deleteAll();

        testUser = userRepository.save(User.builder()
            .email("budget-user@test.com")
            .name("Budget User")
            .passwordHash("$2a$10$dummy")
            .isActive(true)
            .createdAt(Instant.now())
            .build());
    }

    @Test
    void createAndFindById() {
        CreateBudgetRequest request = new CreateBudgetRequest(
            "Q1 Budget 2026",
            new BigDecimal("50000.00"),
            2026,
            "Q1 planning budget",
            null,
            null,
            null
        );

        BudgetResponse created = budgetService.create(request, testUser.getId());
        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("Q1 Budget 2026");
        assertThat(created.amount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(created.status()).isEqualTo("DRAFT");
        assertThat(created.fiscalYear()).isEqualTo(2026);

        BudgetResponse found = budgetService.findById(created.id());
        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.name()).isEqualTo(created.name());
    }

    @Test
    void findAll_withPagination() {
        CreateBudgetRequest req1 = new CreateBudgetRequest("Budget 1", new BigDecimal("1000"), 2026, null, null, null, null);
        CreateBudgetRequest req2 = new CreateBudgetRequest("Budget 2", new BigDecimal("2000"), 2026, null, null, null, null);
        CreateBudgetRequest req3 = new CreateBudgetRequest("Budget 3", new BigDecimal("3000"), 2026, null, null, null, null);

        budgetService.create(req1, testUser.getId());
        budgetService.create(req2, testUser.getId());
        budgetService.create(req3, testUser.getId());

        var result = budgetService.findAll(2026, null, 1, 2);
        assertThat(result).containsKey("data");
        assertThat(result).containsKey("meta");

        @SuppressWarnings("unchecked")
        var data = (java.util.List<BudgetResponse>) result.get("data");
        assertThat(data).hasSize(2);

        @SuppressWarnings("unchecked")
        var meta = (java.util.Map<String, Object>) result.get("meta");
        assertThat(meta.get("total")).isEqualTo(3L);
        assertThat(meta.get("totalPages")).isEqualTo(2);
    }

    @Test
    void submitBudget_transitionsStatusToSubmitted() {
        CreateBudgetRequest request = new CreateBudgetRequest("Submit Test", new BigDecimal("5000"), 2026, null, null, null, null);
        BudgetResponse created = budgetService.create(request, testUser.getId());

        Budget submitted = budgetService.submit(created.id(), testUser.getId());
        assertThat(submitted.getStatus()).isEqualTo("SUBMITTED");
    }

    @Test
    void approveBudget_transitionsStatusToApproved() {
        CreateBudgetRequest request = new CreateBudgetRequest("Approve Test", new BigDecimal("5000"), 2026, null, null, null, null);
        BudgetResponse created = budgetService.create(request, testUser.getId());

        budgetService.submit(created.id(), testUser.getId());
        Budget approved = budgetService.approve(created.id(), testUser.getId());

        assertThat(approved.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void rejectBudget_transitionsStatusToRejected() {
        CreateBudgetRequest request = new CreateBudgetRequest("Reject Test", new BigDecimal("5000"), 2026, null, null, null, null);
        BudgetResponse created = budgetService.create(request, testUser.getId());

        budgetService.submit(created.id(), testUser.getId());
        Budget rejected = budgetService.reject(created.id(), testUser.getId());

        assertThat(rejected.getStatus()).isEqualTo("REJECTED");
    }

    @Test
    void archiveApprovedBudget_transitionsStatusToArchived() {
        CreateBudgetRequest request = new CreateBudgetRequest("Archive Test", new BigDecimal("5000"), 2026, null, null, null, null);
        BudgetResponse created = budgetService.create(request, testUser.getId());

        budgetService.submit(created.id(), testUser.getId());
        budgetService.approve(created.id(), testUser.getId());
        Budget archived = budgetService.archive(created.id());

        assertThat(archived.getStatus()).isEqualTo("ARCHIVED");
    }

    @Test
    void deleteOnlyDraftBudget_throwsForNonDraft() {
        CreateBudgetRequest request = new CreateBudgetRequest("Delete Test", new BigDecimal("5000"), 2026, null, null, null, null);
        BudgetResponse created = budgetService.create(request, testUser.getId());

        budgetService.submit(created.id(), testUser.getId());

        assertThrows(IllegalStateException.class, () -> budgetService.remove(created.id()));
    }

    @Test
    void updateDraftBudget_succeeds() {
        CreateBudgetRequest create = new CreateBudgetRequest("Original", new BigDecimal("5000"), 2026, null, null, null, null);
        BudgetResponse created = budgetService.create(create, testUser.getId());

        UpdateBudgetRequest update = new UpdateBudgetRequest("Updated Name", new BigDecimal("10000"), "New description");
        BudgetResponse updated = budgetService.update(created.id(), update, testUser.getId());

        assertThat(updated.name()).isEqualTo("Updated Name");
        assertThat(updated.amount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(updated.description()).isEqualTo("New description");
    }

    @Test
    void getStatistics_returnsCorrectCounts() {
        budgetService.create(new CreateBudgetRequest("B1", new BigDecimal("1000"), 2026, null, null, null, null), testUser.getId());
        budgetService.create(new CreateBudgetRequest("B2", new BigDecimal("2000"), 2026, null, null, null, null), testUser.getId());

        BudgetStatisticsResponse stats = budgetService.getStatistics(2026);

        assertThat(stats.totalBudgets()).isEqualTo(2);
        assertThat(stats.totalAmount()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(stats.byStatus()).containsKey("DRAFT");
    }
}
