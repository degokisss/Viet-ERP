package com.vieterp.otb.service;

import com.vieterp.otb.approvalworkflow.ApprovalWorkflowService;
import com.vieterp.otb.approvalworkflow.dto.*;
import com.vieterp.otb.approvalworkflow.exception.*;
import com.vieterp.otb.domain.*;
import com.vieterp.otb.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApprovalWorkflowServiceTest {

    @Mock private ApprovalWorkflowRepository approvalWorkflowRepository;
    @Mock private ApprovalWorkflowLevelRepository approvalWorkflowLevelRepository;
    @Mock private GroupBrandRepository groupBrandRepository;
    @Mock private UserRepository userRepository;

    private ApprovalWorkflowService service;

    private User testUser;
    private GroupBrand testGroupBrand;
    private ApprovalWorkflow testWorkflow;
    private ApprovalWorkflowLevel testLevel;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .name("Test User")
            .email("test@vieterp.com")
            .build();

        testGroupBrand = GroupBrand.builder()
            .id(1L)
            .name("Group Brand A")
            .build();

        testWorkflow = ApprovalWorkflow.builder()
            .id(1L)
            .groupBrandId(1L)
            .workflowName("Q1 Approval")
            .createdBy(1L)
            .createdAt(Instant.now())
            .build();

        testLevel = ApprovalWorkflowLevel.builder()
            .id(1L)
            .approvalWorkflowId(1L)
            .levelOrder(1)
            .levelName("Manager")
            .approverUserId(1L)
            .isRequired(true)
            .createdBy(1L)
            .createdAt(Instant.now())
            .build();

        // Common stubs used across multiple tests
        when(approvalWorkflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(groupBrandRepository.findById(1L)).thenReturn(Optional.of(testGroupBrand));

        service = new ApprovalWorkflowService(
            approvalWorkflowRepository,
            approvalWorkflowLevelRepository,
            groupBrandRepository,
            userRepository
        );
    }

    // ─── FIND BY ID ───────────────────────────────────────────────────────────

    @Test
    void findById_existingWorkflow_returnsResponse() {
        when(approvalWorkflowLevelRepository.findByApprovalWorkflowIdOrderByLevelOrder(1L))
            .thenReturn(List.of(testLevel));

        ApprovalWorkflowResponse resp = service.findById(1L);

        assertNotNull(resp);
        assertEquals(1L, resp.id());
        assertEquals("Q1 Approval", resp.workflowName());
    }

    @Test
    void findById_nonExistingWorkflow_throwsException() {
        when(approvalWorkflowRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ApprovalWorkflowNotFoundException.class, () -> service.findById(99L));
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Test
    void create_withValidData_savesWorkflowSuccessfully() {
        CreateApprovalWorkflowRequest req = new CreateApprovalWorkflowRequest(
            "1", "New Workflow", List.of()
        );
        when(approvalWorkflowRepository.save(any(ApprovalWorkflow.class))).thenAnswer(inv -> {
            ApprovalWorkflow w = inv.getArgument(0);
            w.setId(2L);
            return w;
        });
        when(approvalWorkflowRepository.findById(2L)).thenReturn(Optional.of(
            ApprovalWorkflow.builder().id(2L).groupBrandId(1L).workflowName("New Workflow")
                .createdBy(1L).createdAt(Instant.now()).build()));
        when(approvalWorkflowLevelRepository.findByApprovalWorkflowIdOrderByLevelOrder(2L))
            .thenReturn(Collections.emptyList());

        ApprovalWorkflowResponse resp = service.create(req, 1L);

        assertNotNull(resp);
        verify(approvalWorkflowRepository).save(any(ApprovalWorkflow.class));
    }

    @Test
    void create_withLevels_savesLevelsToo() {
        CreateApprovalWorkflowRequest.ApprovalWorkflowLevelRequest levelReq =
            new CreateApprovalWorkflowRequest.ApprovalWorkflowLevelRequest("Manager", "1", true);
        CreateApprovalWorkflowRequest req = new CreateApprovalWorkflowRequest(
            "1", "New Workflow", List.of(levelReq)
        );
        when(approvalWorkflowRepository.save(any(ApprovalWorkflow.class))).thenAnswer(inv -> {
            ApprovalWorkflow w = inv.getArgument(0);
            w.setId(2L);
            return w;
        });
        when(approvalWorkflowRepository.findById(2L)).thenReturn(Optional.of(
            ApprovalWorkflow.builder().id(2L).groupBrandId(1L).workflowName("New Workflow")
                .createdBy(1L).createdAt(Instant.now()).build()));
        when(approvalWorkflowLevelRepository.findByApprovalWorkflowIdOrderByLevelOrder(2L))
            .thenReturn(Collections.emptyList());

        ApprovalWorkflowResponse resp = service.create(req, 1L);

        verify(approvalWorkflowLevelRepository, times(1)).save(any(ApprovalWorkflowLevel.class));
    }

    @Test
    void create_userNotFound_throwsException() {
        CreateApprovalWorkflowRequest req = new CreateApprovalWorkflowRequest(
            "1", "New Workflow", List.of()
        );
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.create(req, 99L));
    }

    // ─── ADD LEVEL ───────────────────────────────────────────────────────────

    @Test
    void addLevel_validRequest_addsLevelToWorkflow() {
        UpdateApprovalWorkflowLevelRequest dto = new UpdateApprovalWorkflowLevelRequest(
            "Senior Manager", null, true
        );
        when(approvalWorkflowLevelRepository.findMaxLevelOrderByWorkflowId(1L)).thenReturn(1);
        when(approvalWorkflowLevelRepository.save(any(ApprovalWorkflowLevel.class))).thenAnswer(inv -> {
            ApprovalWorkflowLevel l = inv.getArgument(0);
            l.setId(2L);
            return l;
        });
        when(approvalWorkflowLevelRepository.findByApprovalWorkflowIdOrderByLevelOrder(1L))
            .thenReturn(List.of(testLevel));

        ApprovalWorkflowResponse resp = service.addLevel(1L, dto, 1L);

        assertNotNull(resp);
        verify(approvalWorkflowLevelRepository).save(any(ApprovalWorkflowLevel.class));
    }

    // ─── REMOVE LEVEL ───────────────────────────────────────────────────────

    @Test
    void removeLevel_existingLevel_deletesLevel() {
        when(approvalWorkflowLevelRepository.findById(1L)).thenReturn(Optional.of(testLevel));
        doNothing().when(approvalWorkflowLevelRepository).delete(testLevel);
        when(approvalWorkflowLevelRepository.findByApprovalWorkflowIdOrderByLevelOrder(1L))
            .thenReturn(Collections.emptyList());
        when(approvalWorkflowLevelRepository.findByApprovalWorkflowIdOrderByLevelOrder(1L))
            .thenReturn(Collections.emptyList());

        ApprovalWorkflowResponse resp = service.removeLevel(1L);

        assertNotNull(resp);
        verify(approvalWorkflowLevelRepository).delete(testLevel);
    }

    @Test
    void removeLevel_nonExistingLevel_throwsException() {
        when(approvalWorkflowLevelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ApprovalWorkflowLevelNotFoundException.class, () -> service.removeLevel(99L));
    }

    // ─── REMOVE WORKFLOW ────────────────────────────────────────────────────

    @Test
    void remove_existingWorkflow_deletesWorkflowAndLevels() {
        when(approvalWorkflowLevelRepository.findByApprovalWorkflowIdOrderByLevelOrder(1L))
            .thenReturn(List.of(testLevel));
        doNothing().when(approvalWorkflowLevelRepository).deleteAll(anyList());
        doNothing().when(approvalWorkflowRepository).delete(testWorkflow);

        assertDoesNotThrow(() -> service.remove(1L));
        verify(approvalWorkflowRepository).delete(testWorkflow);
    }

    @Test
    void remove_nonExistingWorkflow_throwsException() {
        when(approvalWorkflowRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ApprovalWorkflowNotFoundException.class, () -> service.remove(99L));
    }

    // ─── REORDER LEVELS ────────────────────────────────────────────────────

    @Test
    void reorderLevels_validLevelIds_reordersLevels() {
        ApprovalWorkflowLevel level2 = ApprovalWorkflowLevel.builder()
            .id(2L).approvalWorkflowId(1L).levelOrder(2).build();
        when(approvalWorkflowLevelRepository.findById(1L)).thenReturn(Optional.of(testLevel));
        when(approvalWorkflowLevelRepository.findById(2L)).thenReturn(Optional.of(level2));
        when(approvalWorkflowLevelRepository.save(any(ApprovalWorkflowLevel.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(approvalWorkflowLevelRepository.findByApprovalWorkflowIdOrderByLevelOrder(1L))
            .thenReturn(List.of(testLevel, level2));

        ApprovalWorkflowResponse resp = service.reorderLevels(1L, List.of("2", "1"));

        assertNotNull(resp);
        verify(approvalWorkflowLevelRepository, times(2)).save(any(ApprovalWorkflowLevel.class));
    }

    @Test
    void reorderLevels_emptyList_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> service.reorderLevels(1L, List.of()));
    }

    // ─── FIND BY GROUP BRAND ─────────────────────────────────────────────────

    @Test
    void findByGroupBrand_existingGroupBrand_returnsWorkflows() {
        when(approvalWorkflowRepository.findByGroupBrandId(1L))
            .thenReturn(List.of(testWorkflow));
        when(approvalWorkflowLevelRepository.findByApprovalWorkflowIdOrderByLevelOrder(1L))
            .thenReturn(Collections.emptyList());
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(groupBrandRepository.findById(any())).thenReturn(Optional.of(testGroupBrand));

        List<ApprovalWorkflowResponse> resp = service.findByGroupBrand("1");

        assertEquals(1, resp.size());
    }
}
