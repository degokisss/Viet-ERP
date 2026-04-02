package com.vieterp.otb.service;

import com.vieterp.otb.domain.*;
import com.vieterp.otb.domain.repository.*;
import com.vieterp.otb.proposal.ProposalService;
import com.vieterp.otb.proposal.dto.*;
import com.vieterp.otb.proposal.exception.ProposalNotFoundException;
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
class ProposalServiceTest {

    @Mock private SKUProposalHeaderRepository proposalHeaderRepository;
    @Mock private SKUProposalRepository proposalRepository;
    @Mock private SKUAllocateRepository allocateRepository;
    @Mock private ProposalSizingHeaderRepository sizingHeaderRepository;
    @Mock private ProposalSizingRepository sizingRepository;
    @Mock private AllocateHeaderRepository allocateHeaderRepository;
    @Mock private UserRepository userRepository;

    private ProposalService service;

    private User testUser;
    private AllocateHeader testAllocateHeader;
    private Brand testBrand;
    private Budget testBudget;
    private SKUProposalHeader testHeader;

    @BeforeEach
    void setUp() {
        service = new ProposalService(
            proposalHeaderRepository,
            proposalRepository,
            allocateRepository,
            sizingHeaderRepository,
            sizingRepository,
            allocateHeaderRepository,
            userRepository
        );

        testUser = User.builder()
            .id(1L)
            .name("Test User")
            .email("test@vieterp.com")
            .build();

        testBudget = Budget.builder()
            .id(1L)
            .name("Q1 Budget")
            .build();

        testBrand = Brand.builder()
            .id(1L)
            .code("BRAND_A")
            .name("Brand A")
            .build();

        testAllocateHeader = AllocateHeader.builder()
            .id(1L)
            .budget(testBudget)
            .brand(testBrand)
            .version(1)
            .isFinalVersion(false)
            .isSnapshot(false)
            .build();

        testHeader = SKUProposalHeader.builder()
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
    void create_savesProposalHeaderSuccessfully() {
        CreateProposalRequest req = new CreateProposalRequest("1", false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(allocateHeaderRepository.findById(1L)).thenReturn(Optional.of(testAllocateHeader));
        when(proposalHeaderRepository.findAll()).thenReturn(Collections.emptyList());
        when(proposalHeaderRepository.save(any(SKUProposalHeader.class))).thenAnswer(inv -> {
            SKUProposalHeader h = inv.getArgument(0);
            h.setId(2L);
            return h;
        });
        when(proposalHeaderRepository.findById(2L)).thenReturn(Optional.of(
            SKUProposalHeader.builder()
                .id(2L).version(1).status("DRAFT").isFinalVersion(false)
                .allocateHeader(testAllocateHeader)
                .creator(testUser).createdAt(Instant.now()).build()));
        when(proposalRepository.findBySkuProposalHeaderId(2L)).thenReturn(Collections.emptyList());
        when(sizingHeaderRepository.findBySkuProposalHeaderId(2L)).thenReturn(Collections.emptyList());

        ProposalResponse resp = service.create(req, 1L);

        assertNotNull(resp);
        verify(proposalHeaderRepository).save(any(SKUProposalHeader.class));
    }

    @Test
    void create_userNotFound_throwsException() {
        CreateProposalRequest req = new CreateProposalRequest("1", false);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.create(req, 99L));
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @Test
    void update_draftProposal_updatesSuccessfully() {
        UpdateProposalRequest req = new UpdateProposalRequest(null);
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);
        when(proposalHeaderRepository.save(any(SKUProposalHeader.class))).thenAnswer(inv -> inv.getArgument(0));
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(proposalRepository.findBySkuProposalHeaderId(1L)).thenReturn(Collections.emptyList());
        when(sizingHeaderRepository.findBySkuProposalHeaderId(1L)).thenReturn(Collections.emptyList());

        ProposalResponse resp = service.update(1L, req, 1L);

        assertNotNull(resp);
        verify(proposalHeaderRepository).save(any(SKUProposalHeader.class));
    }

    @Test
    void update_nonDraftProposal_throwsException() {
        testHeader.setStatus("SUBMITTED");
        UpdateProposalRequest req = new UpdateProposalRequest(null);
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));

        assertThrows(IllegalStateException.class, () -> service.update(1L, req, 1L));
    }

    // ─── SUBMIT ───────────────────────────────────────────────────────────────

    @Test
    void submit_draftProposal_setsStatusToSubmitted() {
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);
        when(proposalHeaderRepository.save(any(SKUProposalHeader.class))).thenAnswer(inv -> inv.getArgument(0));

        SKUProposalHeader result = service.submit(1L, 1L);

        assertEquals("SUBMITTED", result.getStatus());
    }

    @Test
    void submit_nonDraftProposal_throwsException() {
        testHeader.setStatus("APPROVED");
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));

        assertThrows(IllegalStateException.class, () -> service.submit(1L, 1L));
    }

    // ─── APPROVE ─────────────────────────────────────────────────────────────

    @Test
    void approveByLevel_submittedProposal_setsStatusToApproved() {
        testHeader.setStatus("SUBMITTED");
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);
        when(proposalHeaderRepository.save(any(SKUProposalHeader.class))).thenAnswer(inv -> inv.getArgument(0));

        SKUProposalHeader result = service.approveByLevel(1L, 1, 1L);

        assertEquals("APPROVED", result.getStatus());
    }

    @Test
    void approveByLevel_nonSubmittedProposal_throwsException() {
        testHeader.setStatus("DRAFT");
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));

        assertThrows(IllegalStateException.class, () -> service.approveByLevel(1L, 1, 1L));
    }

    // ─── REJECT ─────────────────────────────────────────────────────────────

    @Test
    void reject_submittedProposal_setsStatusToRejected() {
        testHeader.setStatus("SUBMITTED");
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);
        when(proposalHeaderRepository.save(any(SKUProposalHeader.class))).thenAnswer(inv -> inv.getArgument(0));

        SKUProposalHeader result = service.reject(1L, 1L);

        assertEquals("REJECTED", result.getStatus());
    }

    @Test
    void reject_nonSubmittedProposal_throwsException() {
        testHeader.setStatus("DRAFT");
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));

        assertThrows(IllegalStateException.class, () -> service.reject(1L, 1L));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Test
    void remove_draftProposal_deletesSuccessfully() {
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(proposalRepository.findBySkuProposalHeaderId(1L)).thenReturn(Collections.emptyList());
        when(sizingHeaderRepository.findBySkuProposalHeaderId(1L)).thenReturn(Collections.emptyList());
        doNothing().when(proposalHeaderRepository).delete(testHeader);

        assertDoesNotThrow(() -> service.remove(1L));
        verify(proposalHeaderRepository).delete(testHeader);
    }

    @Test
    void remove_nonDraftProposal_throwsException() {
        testHeader.setStatus("APPROVED");
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));

        assertThrows(IllegalStateException.class, () -> service.remove(1L));
    }

    // ─── ADD PRODUCT ─────────────────────────────────────────────────────────

    @Test
    void addProduct_draftProposal_addsProduct() {
        CreateSKUProposalRequest prodReq = new CreateSKUProposalRequest(
            "1", "Target A", new BigDecimal("10.00"), new BigDecimal("20.00")
        );
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(proposalRepository.save(any(SKUProposal.class))).thenAnswer(inv -> {
            SKUProposal p = inv.getArgument(0);
            p.setId(2L);
            return p;
        });
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);
        when(proposalHeaderRepository.save(any(SKUProposalHeader.class))).thenAnswer(inv -> inv.getArgument(0));
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(proposalRepository.findBySkuProposalHeaderId(1L)).thenReturn(Collections.emptyList());
        when(sizingHeaderRepository.findBySkuProposalHeaderId(1L)).thenReturn(Collections.emptyList());

        ProposalResponse resp = service.addProduct(1L, prodReq, 1L);

        assertNotNull(resp);
        verify(proposalRepository).save(any(SKUProposal.class));
    }

    // ─── COPY PROPOSAL ───────────────────────────────────────────────────────

    @Test
    void copyProposal_createsNewVersion() {
        SKUProposal originalProposal = SKUProposal.builder()
            .id(1L)
            .skuProposalHeaderId(1L)
            .productId(100L)
            .customerTarget("Target A")
            .unitCost(new BigDecimal("10.00"))
            .srp(new BigDecimal("20.00"))
            .createdBy(1L)
            .createdAt(Instant.now())
            .build();

        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(proposalHeaderRepository.save(any(SKUProposalHeader.class))).thenAnswer(inv -> {
            SKUProposalHeader h = inv.getArgument(0);
            h.setId(2L);
            return h;
        });
        when(proposalRepository.findBySkuProposalHeaderId(1L))
            .thenReturn(List.of(originalProposal));
        when(proposalRepository.save(any(SKUProposal.class))).thenAnswer(inv -> {
            SKUProposal p = inv.getArgument(0);
            p.setId(2L);
            return p;
        });
        when(allocateRepository.findBySkuProposalId(1L)).thenReturn(Collections.emptyList());
        when(sizingHeaderRepository.findBySkuProposalHeaderId(1L)).thenReturn(Collections.emptyList());
        when(sizingRepository.findByProposalSizingHeaderId(any())).thenReturn(Collections.emptyList());

        ProposalResponse resp = service.copyProposal(1L, 1L);

        assertNotNull(resp);
        verify(proposalRepository).save(any(SKUProposal.class));
    }

    // ─── FIND BY ID ──────────────────────────────────────────────────────────

    @Test
    void findById_existingHeader_returnsResponse() {
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(proposalRepository.findBySkuProposalHeaderId(1L)).thenReturn(Collections.emptyList());
        when(sizingHeaderRepository.findBySkuProposalHeaderId(1L)).thenReturn(Collections.emptyList());

        ProposalResponse resp = service.findById(1L);

        assertNotNull(resp);
        assertEquals(1L, resp.id());
    }

    @Test
    void findById_nonExistingHeader_throwsException() {
        when(proposalHeaderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProposalNotFoundException.class, () -> service.findById(99L));
    }

    // ─── CREATE SIZING HEADER ──────────────────────────────────────────────

    @Test
    void createSizingHeader_belowMaxAllowsCreation() {
        CreateSizingHeaderRequest req = new CreateSizingHeaderRequest(null);
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(sizingHeaderRepository.countBySkuProposalHeaderId(1L)).thenReturn(0L);
        when(sizingHeaderRepository.save(any(ProposalSizingHeader.class))).thenAnswer(inv -> {
            ProposalSizingHeader h = inv.getArgument(0);
            h.setId(1L);
            return h;
        });
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);
        when(proposalHeaderRepository.save(any(SKUProposalHeader.class))).thenAnswer(inv -> inv.getArgument(0));
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(proposalRepository.findBySkuProposalHeaderId(1L)).thenReturn(Collections.emptyList());
        when(sizingHeaderRepository.findBySkuProposalHeaderId(1L)).thenReturn(Collections.emptyList());

        ProposalResponse resp = service.createSizingHeader(1L, req, 1L);

        assertNotNull(resp);
        verify(sizingHeaderRepository).save(any(ProposalSizingHeader.class));
    }

    @Test
    void createSizingHeader_atMaxThrowsException() {
        CreateSizingHeaderRequest req = new CreateSizingHeaderRequest(null);
        when(proposalHeaderRepository.findById(1L)).thenReturn(Optional.of(testHeader));
        when(sizingHeaderRepository.countBySkuProposalHeaderId(1L)).thenReturn(3L);

        assertThrows(IllegalStateException.class, () -> service.createSizingHeader(1L, req, 1L));
    }
}
