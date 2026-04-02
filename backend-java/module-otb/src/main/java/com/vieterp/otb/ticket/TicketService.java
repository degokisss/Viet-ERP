package com.vieterp.otb.ticket;

import com.vieterp.otb.ticket.dto.*;
import com.vieterp.otb.ticket.exception.*;
import com.vieterp.otb.domain.*;
import com.vieterp.otb.domain.repository.*;
import com.vieterp.otb.budget.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketApprovalLogRepository ticketApprovalLogRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetAllocateRepository budgetAllocateRepository;
    private final AllocateHeaderRepository allocateHeaderRepository;
    private final PlanningHeaderRepository planningHeaderRepository;
    private final SKUProposalHeaderRepository skuProposalHeaderRepository;
    private final ProposalSizingHeaderRepository proposalSizingHeaderRepository;
    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final SeasonGroupRepository seasonGroupRepository;
    private final SeasonRepository seasonRepository;
    private final ApprovalWorkflowRepository approvalWorkflowRepository;
    private final ApprovalWorkflowLevelRepository approvalWorkflowLevelRepository;

    // ─── VALIDATE BUDGET READINESS ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TicketValidationResponse validateBudgetReadiness(String budgetId) {
        List<TicketValidationResponse.ValidationStep> steps = new ArrayList<>();
        Budget budget = budgetRepository.findById(Long.parseLong(budgetId))
            .orElseThrow(() -> new TicketNotFoundException("Budget not found with id: " + budgetId));

        // Step 1: Final AllocateHeader exists for each brand
        steps.add(validateFinalAllocateHeaders(budget));

        // Step 2: Final PlanningHeader exists for each brand
        steps.add(validateFinalPlanningHeaders(budget));

        // Step 3: Final SKUProposalHeader exists for each brand
        steps.add(validateFinalSKUProposalHeaders(budget));

        // Step 4: At least 3 ProposalSizingHeaders per SKUProposalHeader
        steps.add(validateProposalSizingHeaders(budget));

        boolean ready = steps.stream().allMatch(TicketValidationResponse.ValidationStep::passed);

        return TicketValidationResponse.builder()
            .ready(ready)
            .steps(steps)
            .build();
    }

    private TicketValidationResponse.ValidationStep validateFinalAllocateHeaders(Budget budget) {
        List<String> details = new ArrayList<>();
        boolean passed = true;
        String message = "OK";

        List<Brand> brands = brandRepository.findAll();
        for (Brand brand : brands) {
            List<AllocateHeader> finalHeaders = allocateHeaderRepository
                .findFinalVersionByBudgetAndBrand(budget.getId(), brand.getId());
            if (finalHeaders.isEmpty()) {
                passed = false;
                details.add("Missing final AllocateHeader for brand: " + brand.getName());
            }
        }

        if (!passed) {
            message = "Missing final AllocateHeaders for some brands";
        }

        return TicketValidationResponse.ValidationStep.builder()
            .name("Final AllocateHeader per Brand")
            .passed(passed)
            .message(message)
            .details(details)
            .build();
    }

    private TicketValidationResponse.ValidationStep validateFinalPlanningHeaders(Budget budget) {
        List<String> details = new ArrayList<>();
        boolean passed = true;
        String message = "OK";

        List<Brand> brands = brandRepository.findAll();
        for (Brand brand : brands) {
            List<AllocateHeader> finalAllocHeaders = allocateHeaderRepository
                .findFinalVersionByBudgetAndBrand(budget.getId(), brand.getId());
            for (AllocateHeader allocHeader : finalAllocHeaders) {
                List<PlanningHeader> finalPlannings = planningHeaderRepository
                    .findFinalVersionByAllocateHeader(allocHeader.getId());
                if (finalPlannings.isEmpty()) {
                    passed = false;
                    details.add("Missing final PlanningHeader for brand " + brand.getName() + " (allocate v" + allocHeader.getVersion() + ")");
                }
            }
        }

        if (!passed) {
            message = "Missing final PlanningHeaders for some brands";
        }

        return TicketValidationResponse.ValidationStep.builder()
            .name("Final PlanningHeader per Brand")
            .passed(passed)
            .message(message)
            .details(details)
            .build();
    }

    private TicketValidationResponse.ValidationStep validateFinalSKUProposalHeaders(Budget budget) {
        List<String> details = new ArrayList<>();
        boolean passed = true;
        String message = "OK";

        List<Brand> brands = brandRepository.findAll();
        for (Brand brand : brands) {
            List<AllocateHeader> finalAllocHeaders = allocateHeaderRepository
                .findFinalVersionByBudgetAndBrand(budget.getId(), brand.getId());
            for (AllocateHeader allocHeader : finalAllocHeaders) {
                List<SKUProposalHeader> finalProposals = skuProposalHeaderRepository
                    .findFinalVersionByAllocateHeader(allocHeader.getId());
                if (finalProposals.isEmpty()) {
                    passed = false;
                    details.add("Missing final SKUProposalHeader for brand " + brand.getName() + " (allocate v" + allocHeader.getVersion() + ")");
                }
            }
        }

        if (!passed) {
            message = "Missing final SKUProposalHeaders for some brands";
        }

        return TicketValidationResponse.ValidationStep.builder()
            .name("Final SKUProposalHeader per Brand")
            .passed(passed)
            .message(message)
            .details(details)
            .build();
    }

    private TicketValidationResponse.ValidationStep validateProposalSizingHeaders(Budget budget) {
        List<String> details = new ArrayList<>();
        boolean passed = true;
        String message = "OK";

        List<Brand> brands = brandRepository.findAll();
        for (Brand brand : brands) {
            List<AllocateHeader> finalAllocHeaders = allocateHeaderRepository
                .findFinalVersionByBudgetAndBrand(budget.getId(), brand.getId());
            for (AllocateHeader allocHeader : finalAllocHeaders) {
                List<SKUProposalHeader> finalProposals = skuProposalHeaderRepository
                    .findFinalVersionByAllocateHeader(allocHeader.getId());
                for (SKUProposalHeader proposalHeader : finalProposals) {
                    long sizingCount = proposalSizingHeaderRepository.countBySkuProposalHeaderId(proposalHeader.getId());
                    if (sizingCount < 3) {
                        passed = false;
                        details.add("Brand " + brand.getName() + " (allocate v" + allocHeader.getVersion()
                            + "): SKUProposal v" + proposalHeader.getVersion() + " has only " + sizingCount + " sizing headers (min 3)");
                    }
                }
            }
        }

        if (!passed) {
            message = "Some SKUProposalHeaders have fewer than 3 ProposalSizingHeaders";
        }

        return TicketValidationResponse.ValidationStep.builder()
            .name("At least 3 ProposalSizingHeaders per SKUProposalHeader")
            .passed(passed)
            .message(message)
            .details(details)
            .build();
    }

    // ─── LIST ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> findAll(String status, String budgetId, String seasonGroupId,
                                        String seasonId, int page, int pageSize) {
        Specification<Ticket> spec = Specification.where(null);

        if (status != null && !status.isBlank()) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), status));
        }
        if (budgetId != null && !budgetId.isBlank()) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("budget").get("id"), Long.parseLong(budgetId)));
        }
        if (seasonGroupId != null && !seasonGroupId.isBlank()) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("seasonGroup").get("id"), Long.parseLong(seasonGroupId)));
        }
        if (seasonId != null && !seasonId.isBlank()) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("season").get("id"), Long.parseLong(seasonId)));
        }

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Ticket> pageResult = ticketRepository.findAll(spec, pageable);

        List<TicketResponse> data = pageResult.getContent().stream()
            .map(this::toResponse)
            .toList();

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", page);
        meta.put("pageSize", pageSize);
        meta.put("total", pageResult.getTotalElements());
        meta.put("totalPages", pageResult.getTotalPages());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("meta", meta);
        return result;
    }

    // ─── GET ONE ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TicketResponse findById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new TicketNotFoundException(id.toString()));
        return toResponse(ticket);
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    public TicketResponse create(CreateTicketRequest dto, Long userId) {
        User creator = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Budget budget = budgetRepository.findById(Long.parseLong(dto.budgetId()))
            .orElseThrow(() -> new IllegalArgumentException("Budget not found: " + dto.budgetId()));

        Ticket ticket = Ticket.builder()
            .budget(budget)
            .seasonGroup(dto.seasonGroupId() != null ? seasonGroupRepository.getReferenceById(Long.parseLong(dto.seasonGroupId())) : null)
            .season(dto.seasonId() != null ? seasonRepository.getReferenceById(Long.parseLong(dto.seasonId())) : null)
            .status("PENDING")
            .creator(creator)
            .createdAt(Instant.now())
            .build();
        ticket = ticketRepository.save(ticket);

        // Create snapshot copies of all final versions
        createSnapshotCopies(ticket, budget, creator);

        return findById(ticket.getId());
    }

    private void createSnapshotCopies(Ticket ticket, Budget budget, User creator) {
        List<Brand> brands = brandRepository.findAll();

        for (Brand brand : brands) {
            // Find final allocate header
            List<AllocateHeader> finalAllocHeaders = allocateHeaderRepository
                .findFinalVersionByBudgetAndBrand(budget.getId(), brand.getId());

            for (AllocateHeader originalAlloc : finalAllocHeaders) {
                // Create snapshot allocate header
                AllocateHeader snapshotAlloc = AllocateHeader.builder()
                    .budget(budget)
                    .brand(brand)
                    .version(originalAlloc.getVersion())
                    .isFinalVersion(false)
                    .isSnapshot(true)
                    .ticket(ticket)
                    .creator(creator)
                    .createdAt(Instant.now())
                    .build();
                snapshotAlloc = allocateHeaderRepository.save(snapshotAlloc);

                // Copy budget allocates
                if (originalAlloc.getBudgetAllocates() != null) {
                    for (BudgetAllocate originalBa : originalAlloc.getBudgetAllocates()) {
                        BudgetAllocate snapshotBa = BudgetAllocate.builder()
                            .allocateHeader(snapshotAlloc)
                            .store(originalBa.getStore())
                            .seasonGroup(originalBa.getSeasonGroup())
                            .season(originalBa.getSeason())
                            .budgetAmount(originalBa.getBudgetAmount())
                            .creator(creator)
                            .createdAt(Instant.now())
                            .build();
                        snapshotAlloc.getBudgetAllocates().add(snapshotBa);
                    }
                    budgetAllocateRepository.saveAll(snapshotAlloc.getBudgetAllocates());
                }

                // Find and snapshot final planning headers
                List<PlanningHeader> finalPlannings = planningHeaderRepository
                    .findFinalVersionByAllocateHeader(originalAlloc.getId());
                for (PlanningHeader originalPlanning : finalPlannings) {
                    PlanningHeader snapshotPlanning = PlanningHeader.builder()
                        .allocateHeader(snapshotAlloc)
                        .version(originalPlanning.getVersion())
                        .status(originalPlanning.getStatus())
                        .isFinalVersion(false)
                        .creator(creator)
                        .createdAt(Instant.now())
                        .build();
                    planningHeaderRepository.save(snapshotPlanning);
                }

                // Find and snapshot final SKU proposal headers
                List<SKUProposalHeader> finalProposals = skuProposalHeaderRepository
                    .findFinalVersionByAllocateHeader(originalAlloc.getId());
                for (SKUProposalHeader originalProposal : finalProposals) {
                    SKUProposalHeader snapshotProposal = SKUProposalHeader.builder()
                        .allocateHeader(snapshotAlloc)
                        .version(originalProposal.getVersion())
                        .status(originalProposal.getStatus())
                        .isFinalVersion(false)
                        .creator(creator)
                        .createdAt(Instant.now())
                        .build();
                    snapshotProposal = skuProposalHeaderRepository.save(snapshotProposal);

                    // Find and snapshot proposal sizing headers
                    List<ProposalSizingHeader> sizings = proposalSizingHeaderRepository
                        .findBySkuProposalHeaderId(originalProposal.getId());
                    for (ProposalSizingHeader originalSizing : sizings) {
                        ProposalSizingHeader snapshotSizing = ProposalSizingHeader.builder()
                            .skuProposalHeaderId(snapshotProposal.getId())
                            .version(originalSizing.getVersion())
                            .isFinalVersion(false)
                            .createdBy(userRepository.getReferenceById(creator.getId()).getId())
                            .createdAt(Instant.now())
                            .build();
                        proposalSizingHeaderRepository.save(snapshotSizing);
                    }
                }
            }
        }
    }

    // ─── PROCESS APPROVAL ─────────────────────────────────────────────────────

    public ProcessApprovalResponse processApproval(Long ticketId, ProcessApprovalRequest dto, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId.toString()));

        if (!"PENDING".equals(ticket.getStatus()) && !"IN_PROGRESS".equals(ticket.getStatus())) {
            throw new IllegalStateException("Ticket is not in a pending or in-progress state: " + ticket.getStatus());
        }

        User approver = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Create approval log
        TicketApprovalLog log = TicketApprovalLog.builder()
            .ticketId(ticketId)
            .approvalWorkflowLevelId(Long.parseLong(dto.approvalWorkflowLevelId()))
            .approverUserId(userId)
            .isApproved(dto.isApproved())
            .comment(dto.comment())
            .approvedAt(Instant.now())
            .createdBy(userId)
            .createdAt(Instant.now())
            .build();
        log = ticketApprovalLogRepository.save(log);

        // Update ticket status
        String newStatus;
        if (Boolean.TRUE.equals(dto.isApproved())) {
            newStatus = "APPROVED";
        } else {
            newStatus = "REJECTED";
        }
        ticket.setStatus(newStatus);
        ticket.setUpdater(approver);
        ticket.setUpdatedAt(Instant.now());
        ticketRepository.save(ticket);

        ProcessApprovalResponse.TicketApprovalLogResponse logResponse = ProcessApprovalResponse.TicketApprovalLogResponse.builder()
            .id(log.getId())
            .approvalWorkflowLevelId(log.getApprovalWorkflowLevelId())
            .approverUserId(log.getApproverUserId())
            .isApproved(log.getIsApproved())
            .comment(log.getComment())
            .approvedAt(log.getApprovedAt())
            .build();

        return ProcessApprovalResponse.builder()
            .log(logResponse)
            .newStatus(newStatus)
            .build();
    }

    // ─── GET APPROVAL HISTORY ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TicketResponse.TicketApprovalLogResponse> getApprovalHistory(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId.toString()));

        List<TicketApprovalLog> logs = ticketApprovalLogRepository.findByTicketIdOrderByApprovedAtDesc(ticketId);

        return logs.stream().map(log -> {
            String levelName = null;
            String approverUserName = null;

            if (log.getApprovalWorkflowLevelId() != null) {
                ApprovalWorkflowLevel level = approvalWorkflowLevelRepository.findById(log.getApprovalWorkflowLevelId()).orElse(null);
                if (level != null) {
                    levelName = level.getLevelName();
                }
            }

            if (log.getApproverUserId() != null) {
                User approver = userRepository.findById(log.getApproverUserId()).orElse(null);
                if (approver != null) {
                    approverUserName = approver.getName();
                }
            }

            return TicketResponse.TicketApprovalLogResponse.builder()
                .id(log.getId())
                .approvalWorkflowLevelId(log.getApprovalWorkflowLevelId())
                .levelName(levelName)
                .approverUserId(log.getApproverUserId())
                .approverUserName(approverUserName)
                .isApproved(log.getIsApproved())
                .comment(log.getComment())
                .approvedAt(log.getApprovedAt())
                .build();
        }).toList();
    }

    // ─── STATISTICS ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TicketStatisticsResponse getStatistics() {
        List<Ticket> allTickets = ticketRepository.findAll();

        long total = allTickets.size();
        Map<String, Long> byStatus = allTickets.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                t -> t.getStatus() != null ? t.getStatus() : "UNKNOWN",
                java.util.stream.Collectors.counting()
            ));

        long pending = allTickets.stream()
            .filter(t -> "PENDING".equals(t.getStatus()) || "IN_PROGRESS".equals(t.getStatus()))
            .count();
        long approved = allTickets.stream()
            .filter(t -> "APPROVED".equals(t.getStatus()))
            .count();
        long rejected = allTickets.stream()
            .filter(t -> "REJECTED".equals(t.getStatus()))
            .count();

        return TicketStatisticsResponse.builder()
            .totalTickets(total)
            .byStatus(byStatus)
            .pendingApprovals(pending)
            .approvedTickets(approved)
            .rejectedTickets(rejected)
            .build();
    }

    // ─── MAPPING ──────────────────────────────────────────────────────────────

    private TicketResponse toResponse(Ticket ticket) {
        List<TicketApprovalLog> logs = ticketApprovalLogRepository.findByTicketIdOrderByApprovedAtDesc(ticket.getId());

        List<TicketResponse.TicketApprovalLogResponse> approvalHistory = logs.stream().map(log -> {
            String levelName = null;
            String approverUserName = null;

            if (log.getApprovalWorkflowLevelId() != null) {
                ApprovalWorkflowLevel level = approvalWorkflowLevelRepository.findById(log.getApprovalWorkflowLevelId()).orElse(null);
                if (level != null) {
                    levelName = level.getLevelName();
                }
            }

            if (log.getApproverUserId() != null) {
                User approver = userRepository.findById(log.getApproverUserId()).orElse(null);
                if (approver != null) {
                    approverUserName = approver.getName();
                }
            }

            return TicketResponse.TicketApprovalLogResponse.builder()
                .id(log.getId())
                .approvalWorkflowLevelId(log.getApprovalWorkflowLevelId())
                .levelName(levelName)
                .approverUserId(log.getApproverUserId())
                .approverUserName(approverUserName)
                .isApproved(log.getIsApproved())
                .comment(log.getComment())
                .approvedAt(log.getApprovedAt())
                .build();
        }).toList();

        String budgetName = null;
        if (ticket.getBudget() != null) {
            budgetName = ticket.getBudget().getName();
        }

        String seasonGroupName = null;
        if (ticket.getSeasonGroup() != null) {
            seasonGroupName = ticket.getSeasonGroup().getName();
        }

        String seasonName = null;
        if (ticket.getSeason() != null) {
            seasonName = ticket.getSeason().getName();
        }

        TicketResponse.CreatorSummary creatorSummary = null;
        if (ticket.getCreator() != null) {
            User c = ticket.getCreator();
            creatorSummary = TicketResponse.CreatorSummary.builder()
                .id(c.getId())
                .name(c.getName())
                .email(c.getEmail())
                .build();
        }

        return TicketResponse.builder()
            .id(ticket.getId())
            .budgetId(ticket.getBudget() != null ? ticket.getBudget().getId() : null)
            .budgetName(budgetName)
            .seasonGroupId(ticket.getSeasonGroup() != null ? ticket.getSeasonGroup().getId() : null)
            .seasonGroupName(seasonGroupName)
            .seasonId(ticket.getSeason() != null ? ticket.getSeason().getId() : null)
            .seasonName(seasonName)
            .status(ticket.getStatus())
            .createdAt(ticket.getCreatedAt())
            .updatedAt(ticket.getUpdatedAt())
            .creator(creatorSummary)
            .approvalHistory(approvalHistory)
            .build();
    }
}
