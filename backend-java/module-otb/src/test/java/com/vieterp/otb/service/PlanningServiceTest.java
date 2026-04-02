package com.vieterp.otb.service;

import com.vieterp.otb.domain.*;
import com.vieterp.otb.domain.repository.*;
import com.vieterp.otb.planning.PlanningService;
import com.vieterp.otb.planning.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanningServiceTest {

    @Mock private PlanningHeaderRepository planningHeaderRepository;
    @Mock private AllocateHeaderRepository allocateHeaderRepository;
    @Mock private PlanningCollectionRepository planningCollectionRepository;
    @Mock private PlanningGenderRepository planningGenderRepository;
    @Mock private PlanningCategoryRepository planningCategoryRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private UserRepository userRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private GenderRepository genderRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private SubCategoryRepository subCategoryRepository;
    @Mock private SeasonTypeRepository seasonTypeRepository;
    @Mock private SeasonGroupRepository seasonGroupRepository;
    @Mock private SeasonRepository seasonRepository;
    @Mock private BudgetAllocateRepository budgetAllocateRepository;

    private PlanningService service;

    private User testUser;
    private Brand testBrand;
    private Budget testBudget;
    private AllocateHeader testAllocateHeader;
    private PlanningHeader testHeader;

    @BeforeEach
    void setUp() {
        service = new PlanningService(
            planningHeaderRepository,
            allocateHeaderRepository,
            planningCollectionRepository,
            planningGenderRepository,
            planningCategoryRepository,
            brandRepository,
            userRepository,
            storeRepository,
            genderRepository,
            categoryRepository,
            subCategoryRepository,
            seasonTypeRepository,
            seasonGroupRepository,
            seasonRepository,
            budgetAllocateRepository
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
            .fiscalYear(2026)
            .build();

        testAllocateHeader = AllocateHeader.builder()
            .id(1L)
            .budget(testBudget)
            .brand(testBrand)
            .version(1)
            .isFinalVersion(false)
            .isSnapshot(false)
            .build();

        testHeader = PlanningHeader.builder()
            .id(1L)
            .allocateHeader(testAllocateHeader)
            .version(1)
            .status("DRAFT")
            .isFinalVersion(false)
            .creator(testUser)
            .createdAt(Instant.now())
            .build();
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Test
    void create_savesPlanningHeaderSuccessfully() {
        CreatePlanningRequest req = new CreatePlanningRequest(
            "1", null, null, null
        );
        when(allocateHeaderRepository.findById(1L)).thenReturn(Optional.of(testAllocateHeader));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(planningHeaderRepository.findByBrandIdOrderByVersionDesc(1L))
            .thenReturn(Collections.emptyList());
        when(planningHeaderRepository.save(any(PlanningHeader.class))).thenAnswer(inv -> {
            PlanningHeader h = inv.getArgument(0);
            h.setId(2L);
            return h;
        });
        when(planningHeaderRepository.findByIdWithHeader(2L)).thenReturn(Optional.of(
            PlanningHeader.builder()
                .id(2L).version(1).status("DRAFT").isFinalVersion(false)
                .allocateHeader(testAllocateHeader)
                .creator(testUser).createdAt(Instant.now()).build()));
        when(planningCollectionRepository.findByPlanningHeaderId(2L)).thenReturn(Collections.emptyList());
        when(planningGenderRepository.findByPlanningHeaderId(2L)).thenReturn(Collections.emptyList());
        when(planningCategoryRepository.findByPlanningHeaderId(2L)).thenReturn(Collections.emptyList());
        when(seasonTypeRepository.findAll()).thenReturn(Collections.emptyList());
        when(storeRepository.findAll()).thenReturn(Collections.emptyList());
        when(genderRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(subCategoryRepository.findAll()).thenReturn(Collections.emptyList());

        PlanningResponse resp = service.create(req, 1L);

        assertNotNull(resp);
        verify(planningHeaderRepository).save(any(PlanningHeader.class));
    }

    @Test
    void create_allocateHeaderNotFound_throwsException() {
        CreatePlanningRequest req = new CreatePlanningRequest("99", null, null, null);
        when(allocateHeaderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.create(req, 1L));
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @Test
    void update_draftPlanning_updatesSuccessfully() {
        UpdatePlanningRequest req = new UpdatePlanningRequest(null, null, null, null);
        when(planningHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);
        when(planningHeaderRepository.save(any(PlanningHeader.class))).thenAnswer(inv -> inv.getArgument(0));
        when(planningHeaderRepository.findByIdWithHeader(1L)).thenReturn(Optional.of(testHeader));
        when(planningCollectionRepository.findByPlanningHeaderId(1L)).thenReturn(Collections.emptyList());
        when(planningGenderRepository.findByPlanningHeaderId(1L)).thenReturn(Collections.emptyList());
        when(planningCategoryRepository.findByPlanningHeaderId(1L)).thenReturn(Collections.emptyList());
        when(seasonTypeRepository.findAll()).thenReturn(Collections.emptyList());
        when(storeRepository.findAll()).thenReturn(Collections.emptyList());
        when(genderRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(subCategoryRepository.findAll()).thenReturn(Collections.emptyList());

        PlanningResponse resp = service.update(1L, req, 1L);

        assertNotNull(resp);
        verify(planningHeaderRepository).save(any(PlanningHeader.class));
    }

    @Test
    void update_nonDraftPlanning_throwsException() {
        testHeader.setStatus("SUBMITTED");
        UpdatePlanningRequest req = new UpdatePlanningRequest(null, null, null, null);
        when(planningHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));

        assertThrows(IllegalStateException.class, () -> service.update(1L, req, 1L));
    }

    // ─── SUBMIT ───────────────────────────────────────────────────────────────

    @Test
    void submit_draftPlanning_setsStatusToSubmitted() {
        when(planningHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);
        when(planningHeaderRepository.save(any(PlanningHeader.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanningHeader result = service.submit(1L, 1L);

        assertEquals("SUBMITTED", result.getStatus());
    }

    @Test
    void submit_nonDraftPlanning_throwsException() {
        testHeader.setStatus("APPROVED");
        when(planningHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));

        assertThrows(IllegalStateException.class, () -> service.submit(1L, 1L));
    }

    // ─── APPROVE BY LEVEL ───────────────────────────────────────────────────

    @Test
    void approveByLevel_submittedPlanning_approvesSuccessfully() {
        testHeader.setStatus("SUBMITTED");
        when(planningHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);
        when(planningHeaderRepository.save(any(PlanningHeader.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanningHeader result = service.approveByLevel(1L, "1", "approve", null, 1L);

        assertEquals("APPROVED", result.getStatus());
    }

    @Test
    void approveByLevel_submittedPlanning_rejectsSuccessfully() {
        testHeader.setStatus("SUBMITTED");
        when(planningHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);
        when(planningHeaderRepository.save(any(PlanningHeader.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanningHeader result = service.approveByLevel(1L, "1", "REJECTED", null, 1L);

        assertEquals("REJECTED", result.getStatus());
    }

    // ─── FINALIZE ────────────────────────────────────────────────────────────

    @Test
    void finalize_planning_setsFinalVersion() {
        when(planningHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);
        when(planningHeaderRepository.save(any(PlanningHeader.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanningHeader result = service.finalize(1L, 1L);

        assertTrue(result.getIsFinalVersion());
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Test
    void remove_draftPlanning_deletesSuccessfully() {
        when(planningHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        doNothing().when(planningHeaderRepository).delete(testHeader);

        assertDoesNotThrow(() -> service.remove(1L));
        verify(planningHeaderRepository).delete(testHeader);
    }

    @Test
    void remove_nonDraftPlanning_throwsException() {
        testHeader.setStatus("APPROVED");
        when(planningHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));

        assertThrows(IllegalStateException.class, () -> service.remove(1L));
    }

    // ─── FIND ONE ───────────────────────────────────────────────────────────

    @Test
    void findOne_existingPlanning_returnsResponse() {
        when(planningHeaderRepository.findByIdWithHeader(1L)).thenReturn(Optional.of(testHeader));
        when(planningCollectionRepository.findByPlanningHeaderId(1L)).thenReturn(Collections.emptyList());
        when(planningGenderRepository.findByPlanningHeaderId(1L)).thenReturn(Collections.emptyList());
        when(planningCategoryRepository.findByPlanningHeaderId(1L)).thenReturn(Collections.emptyList());
        when(seasonTypeRepository.findAll()).thenReturn(Collections.emptyList());
        when(storeRepository.findAll()).thenReturn(Collections.emptyList());
        when(genderRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(subCategoryRepository.findAll()).thenReturn(Collections.emptyList());

        PlanningResponse resp = service.findOne(1L);

        assertNotNull(resp);
        assertEquals(1L, resp.id());
    }

    @Test
    void findOne_nonExistingPlanning_throwsException() {
        when(planningHeaderRepository.findByIdWithHeader(99L)).thenReturn(Optional.empty());

        assertThrows(PlanningService.PlanningNotFoundException.class, () -> service.findOne(99L));
    }
}
