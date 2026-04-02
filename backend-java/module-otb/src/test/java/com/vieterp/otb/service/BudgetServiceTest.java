package com.vieterp.otb.service;

import com.vieterp.otb.budget.BudgetService;
import com.vieterp.otb.budget.domain.dto.*;
import com.vieterp.otb.budget.exception.BudgetNotFoundException;
import com.vieterp.otb.budget.repository.BudgetRepository;
import com.vieterp.otb.domain.*;
import com.vieterp.otb.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private AllocateHeaderRepository allocateHeaderRepository;
    @Mock private BudgetAllocateRepository budgetAllocateRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private UserRepository userRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private SeasonGroupRepository seasonGroupRepository;
    @Mock private SeasonRepository seasonRepository;

    private BudgetService budgetService;

    private User testUser;
    private Brand testBrand;
    private Budget testBudget;
    private AllocateHeader testHeader;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(
            budgetRepository,
            allocateHeaderRepository,
            budgetAllocateRepository,
            brandRepository,
            userRepository,
            storeRepository,
            seasonGroupRepository,
            seasonRepository
        );

        testUser = User.builder()
            .id(1L)
            .name("Test User")
            .email("test@vieterp.com")
            .build();

        testBrand = Brand.builder()
            .id(1L)
            .code("BRAND_A")
            .name("Brand A")
            .build();

        testBudget = Budget.builder()
            .id(1L)
            .name("Q1 Budget")
            .amount(new BigDecimal("1000000"))
            .description("Q1 2026 Budget")
            .status("DRAFT")
            .fiscalYear(2026)
            .creator(testUser)
            .createdAt(Instant.now())
            .allocateHeaders(new ArrayList<>())
            .updater(testUser)
            .updatedAt(Instant.now())
            .build();

        testHeader = AllocateHeader.builder()
            .id(1L)
            .budget(testBudget)
            .brand(testBrand)
            .version(1)
            .isFinalVersion(false)
            .isSnapshot(false)
            .creator(testUser)
            .createdAt(Instant.now())
            .budgetAllocates(new ArrayList<>())
            .updater(testUser)
            .updatedAt(Instant.now())
            .build();

        testBudget.setAllocateHeaders(new ArrayList<>(List.of(testHeader)));
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Test
    void create_savesBudgetSuccessfully() {
        CreateBudgetRequest req = new CreateBudgetRequest(
            "New Budget",
            new BigDecimal("500000"),
            2026,
            "Description",
            null,
            null,
            null
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> {
            Budget b = inv.getArgument(0);
            b.setId(2L);
            return b;
        });
        when(budgetRepository.findByIdWithAllocations(2L)).thenReturn(Optional.of(testBudget));

        BudgetResponse resp = budgetService.create(req, 1L);

        assertNotNull(resp);
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void create_withBrandId_createsAllocateHeader() {
        CreateBudgetRequest req = new CreateBudgetRequest(
            "New Budget",
            new BigDecimal("500000"),
            2026,
            "Description",
            "1",
            null,
            true
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> {
            Budget b = inv.getArgument(0);
            b.setId(2L);
            return b;
        });
        when(allocateHeaderRepository.findLatestByBudgetAndBrand(2L, 1L)).thenReturn(Optional.empty());
        when(allocateHeaderRepository.save(any(AllocateHeader.class))).thenReturn(testHeader);
        when(budgetRepository.findByIdWithAllocations(2L)).thenReturn(Optional.of(testBudget));

        BudgetResponse resp = budgetService.create(req, 1L);

        assertNotNull(resp);
        verify(allocateHeaderRepository).save(any(AllocateHeader.class));
    }

    @Test
    void create_userNotFound_throwsException() {
        CreateBudgetRequest req = new CreateBudgetRequest(
            "New Budget", new BigDecimal("500000"), 2026, "Desc", null, null, null
        );
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> budgetService.create(req, 99L));
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @Test
    void update_editsBudgetSuccessfully() {
        UpdateBudgetRequest req = new UpdateBudgetRequest("Updated Name", new BigDecimal("600000"), "Updated Desc");
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);
        when(budgetRepository.findByIdWithAllocations(1L)).thenReturn(Optional.of(testBudget));

        BudgetResponse resp = budgetService.update(1L, req, 1L);

        assertNotNull(resp);
        assertEquals("Updated Name", testBudget.getName());
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void update_nonDraftBudget_throwsException() {
        testBudget.setStatus("SUBMITTED");
        UpdateBudgetRequest req = new UpdateBudgetRequest("Updated", null, null);
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));

        assertThrows(IllegalStateException.class, () -> budgetService.update(1L, req, 1L));
    }

    @Test
    void update_budgetNotFound_throwsException() {
        UpdateBudgetRequest req = new UpdateBudgetRequest("Updated", null, null);
        when(budgetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BudgetNotFoundException.class, () -> budgetService.update(99L, req, 1L));
    }

    // ─── SUBMIT ───────────────────────────────────────────────────────────────

    @Test
    void submit_draftBudget_setsStatusToSubmitted() {
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        Budget result = budgetService.submit(1L, 1L);

        assertEquals("SUBMITTED", result.getStatus());
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void submit_nonDraftBudget_throwsException() {
        testBudget.setStatus("APPROVED");
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));

        assertThrows(IllegalStateException.class, () -> budgetService.submit(1L, 1L));
    }

    // ─── APPROVE ──────────────────────────────────────────────────────────────

    @Test
    void approve_submittedBudget_setsStatusToApproved() {
        testBudget.setStatus("SUBMITTED");
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        Budget result = budgetService.approve(1L, 1L);

        assertEquals("APPROVED", result.getStatus());
    }

    @Test
    void approve_nonSubmittedBudget_throwsException() {
        testBudget.setStatus("DRAFT");
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));

        assertThrows(IllegalStateException.class, () -> budgetService.approve(1L, 1L));
    }

    // ─── REJECT ───────────────────────────────────────────────────────────────

    @Test
    void reject_submittedBudget_setsStatusToRejected() {
        testBudget.setStatus("SUBMITTED");
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        Budget result = budgetService.reject(1L, 1L);

        assertEquals("REJECTED", result.getStatus());
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Test
    void remove_draftBudget_deletesSuccessfully() {
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));
        doNothing().when(budgetRepository).delete(testBudget);

        assertDoesNotThrow(() -> budgetService.remove(1L));
        verify(budgetRepository).delete(testBudget);
    }

    @Test
    void remove_nonDraftBudget_throwsException() {
        testBudget.setStatus("APPROVED");
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));

        assertThrows(IllegalStateException.class, () -> budgetService.remove(1L));
    }

    // ─── ARCHIVE ───────────────────────────────────────────────────────────────

    @Test
    void archive_approvedBudget_setsStatusToArchived() {
        testBudget.setStatus("APPROVED");
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        Budget result = budgetService.archive(1L);

        assertEquals("ARCHIVED", result.getStatus());
    }

    @Test
    void archive_nonApprovedBudget_throwsException() {
        testBudget.setStatus("SUBMITTED");
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));

        assertThrows(IllegalStateException.class, () -> budgetService.archive(1L));
    }

    // ─── SET FINAL VERSION ─────────────────────────────────────────────────────

    @Test
    void setFinalVersion_setsHeaderToFinalAndUnsetsOthers() {
        AllocateHeader otherHeader = AllocateHeader.builder()
            .id(2L)
            .budget(testBudget)
            .brand(testBrand)
            .version(2)
            .isFinalVersion(true)
            .isSnapshot(false)
            .build();

        testHeader.setIsFinalVersion(false);
        when(allocateHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(allocateHeaderRepository.findOthersByBudgetAndBrand(1L, 1L, 1L))
            .thenReturn(List.of(otherHeader));
        when(allocateHeaderRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(allocateHeaderRepository.save(any(AllocateHeader.class))).thenAnswer(inv -> inv.getArgument(0));
        when(budgetRepository.findByIdWithAllocations(1L)).thenReturn(Optional.of(testBudget));

        BudgetResponse resp = budgetService.setFinalVersion(1L);

        assertNotNull(resp);
        assertTrue(testHeader.getIsFinalVersion()); // the target header is set to final
        assertFalse(otherHeader.getIsFinalVersion()); // other header is unset
        verify(allocateHeaderRepository).save(testHeader);
    }

    // ─── FIND BY ID ───────────────────────────────────────────────────────────

    @Test
    void findById_existingBudget_returnsResponse() {
        testBudget.setAllocateHeaders(List.of(testHeader));
        when(budgetRepository.findByIdWithAllocations(1L)).thenReturn(Optional.of(testBudget));

        BudgetResponse resp = budgetService.findById(1L);

        assertNotNull(resp);
        assertEquals(1L, resp.id());
    }

    @Test
    void findById_nonExistingBudget_throwsException() {
        when(budgetRepository.findByIdWithAllocations(99L)).thenReturn(Optional.empty());

        assertThrows(BudgetNotFoundException.class, () -> budgetService.findById(99L));
    }
}
