package com.vieterp.otb.service;

import com.vieterp.otb.budget.repository.BudgetRepository;
import com.vieterp.otb.domain.*;
import com.vieterp.otb.domain.repository.*;
import com.vieterp.otb.ticket.TicketService;
import com.vieterp.otb.ticket.dto.*;
import com.vieterp.otb.ticket.exception.TicketNotFoundException;
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
class TicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private TicketApprovalLogRepository ticketApprovalLogRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private BudgetAllocateRepository budgetAllocateRepository;
    @Mock private AllocateHeaderRepository allocateHeaderRepository;
    @Mock private PlanningHeaderRepository planningHeaderRepository;
    @Mock private SKUProposalHeaderRepository skuProposalHeaderRepository;
    @Mock private ProposalSizingHeaderRepository proposalSizingHeaderRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private UserRepository userRepository;
    @Mock private SeasonGroupRepository seasonGroupRepository;
    @Mock private SeasonRepository seasonRepository;
    @Mock private ApprovalWorkflowRepository approvalWorkflowRepository;
    @Mock private ApprovalWorkflowLevelRepository approvalWorkflowLevelRepository;

    private TicketService service;

    private User testUser;
    private Budget testBudget;
    private SeasonGroup testSeasonGroup;
    private Season testSeason;
    private Ticket testTicket;

    @BeforeEach
    void setUp() {
        service = new TicketService(
            ticketRepository,
            ticketApprovalLogRepository,
            budgetRepository,
            budgetAllocateRepository,
            allocateHeaderRepository,
            planningHeaderRepository,
            skuProposalHeaderRepository,
            proposalSizingHeaderRepository,
            brandRepository,
            userRepository,
            seasonGroupRepository,
            seasonRepository,
            approvalWorkflowRepository,
            approvalWorkflowLevelRepository
        );

        testUser = User.builder()
            .id(1L)
            .name("Test User")
            .email("test@vieterp.com")
            .build();

        testBudget = Budget.builder()
            .id(1L)
            .name("Q1 Budget")
            .amount(new BigDecimal("1000000"))
            .fiscalYear(2026)
            .build();

        testSeasonGroup = SeasonGroup.builder()
            .id(1L)
            .name("Group 1")
            .build();

        testSeason = Season.builder()
            .id(1L)
            .name("S1")
            .build();

        testTicket = Ticket.builder()
            .id(1L)
            .budget(testBudget)
            .seasonGroup(testSeasonGroup)
            .season(testSeason)
            .status("PENDING")
            .creator(testUser)
            .createdAt(Instant.now())
            .build();
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Test
    void create_savesTicketSuccessfully() {
        CreateTicketRequest req = new CreateTicketRequest("1", null, null, "1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(2L);
            return t;
        });
        when(ticketRepository.findById(2L)).thenReturn(Optional.of(
            Ticket.builder()
                .id(2L).budget(testBudget).status("PENDING")
                .creator(testUser).createdAt(Instant.now()).build()));
        when(ticketApprovalLogRepository.findByTicketIdOrderByApprovedAtDesc(2L))
            .thenReturn(Collections.emptyList());
        when(brandRepository.findAll()).thenReturn(Collections.emptyList());

        TicketResponse resp = service.create(req, 1L);

        assertNotNull(resp);
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void create_budgetNotFound_throwsException() {
        CreateTicketRequest req = new CreateTicketRequest("99", null, null, "1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(budgetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.create(req, 1L));
    }

    // ─── PROCESS APPROVAL ───────────────────────────────────────────────────

    @Test
    void processApproval_pendingTicket_approvesSuccessfully() {
        ProcessApprovalRequest dto = new ProcessApprovalRequest("1", true, null);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(ticketApprovalLogRepository.save(any(TicketApprovalLog.class))).thenAnswer(inv -> {
            TicketApprovalLog l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessApprovalResponse resp = service.processApproval(1L, dto, 1L);

        assertEquals("APPROVED", resp.newStatus());
        assertTrue(resp.log().isApproved());
    }

    @Test
    void processApproval_pendingTicket_rejectsSuccessfully() {
        ProcessApprovalRequest dto = new ProcessApprovalRequest("1", false, "Not good enough");
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(ticketApprovalLogRepository.save(any(TicketApprovalLog.class))).thenAnswer(inv -> {
            TicketApprovalLog l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessApprovalResponse resp = service.processApproval(1L, dto, 1L);

        assertEquals("REJECTED", resp.newStatus());
        assertFalse(resp.log().isApproved());
        assertEquals("Not good enough", resp.log().comment());
    }

    @Test
    void processApproval_nonPendingTicket_throwsException() {
        testTicket.setStatus("APPROVED");
        ProcessApprovalRequest dto = new ProcessApprovalRequest("1", true, null);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

        assertThrows(IllegalStateException.class, () -> service.processApproval(1L, dto, 1L));
    }

    // ─── GET APPROVAL HISTORY ───────────────────────────────────────────────

    @Test
    void getApprovalHistory_existingTicket_returnsLogs() {
        TicketApprovalLog log = TicketApprovalLog.builder()
            .id(1L)
            .ticketId(1L)
            .approvalWorkflowLevelId(1L)
            .approverUserId(1L)
            .isApproved(true)
            .comment("LGTM")
            .approvedAt(Instant.now())
            .createdBy(1L)
            .createdAt(Instant.now())
            .build();

        ApprovalWorkflowLevel level = ApprovalWorkflowLevel.builder()
            .id(1L).levelName("Manager").build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
        when(ticketApprovalLogRepository.findByTicketIdOrderByApprovedAtDesc(1L))
            .thenReturn(List.of(log));
        when(approvalWorkflowLevelRepository.findById(1L)).thenReturn(Optional.of(level));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        List<TicketResponse.TicketApprovalLogResponse> history = service.getApprovalHistory(1L);

        assertEquals(1, history.size());
        assertEquals("Manager", history.get(0).levelName());
    }

    // ─── FIND BY ID ───────────────────────────────────────────────────────────

    @Test
    void findById_existingTicket_returnsResponse() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
        when(ticketApprovalLogRepository.findByTicketIdOrderByApprovedAtDesc(1L))
            .thenReturn(Collections.emptyList());

        TicketResponse resp = service.findById(1L);

        assertNotNull(resp);
        assertEquals(1L, resp.id());
    }

    @Test
    void findById_nonExistingTicket_throwsException() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> service.findById(99L));
    }

    // ─── STATISTICS ─────────────────────────────────────────────────────────

    @Test
    void getStatistics_returnsCorrectCounts() {
        Ticket approved = Ticket.builder()
            .id(2L).status("APPROVED").budget(testBudget).creator(testUser)
            .createdAt(Instant.now()).build();
        Ticket rejected = Ticket.builder()
            .id(3L).status("REJECTED").budget(testBudget).creator(testUser)
            .createdAt(Instant.now()).build();

        when(ticketRepository.findAll()).thenReturn(List.of(testTicket, approved, rejected));

        TicketStatisticsResponse stats = service.getStatistics();

        assertEquals(3, stats.totalTickets());
        assertEquals(1L, stats.approvedTickets());
        assertEquals(1L, stats.rejectedTickets());
        assertEquals(1L, stats.pendingApprovals()); // PENDING = 1
    }
}
