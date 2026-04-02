package com.vieterp.otb.approvalworkflow;

import com.vieterp.otb.approvalworkflow.dto.*;
import com.vieterp.otb.approvalworkflow.exception.*;
import com.vieterp.otb.domain.*;
import com.vieterp.otb.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalWorkflowService {

    private final ApprovalWorkflowRepository approvalWorkflowRepository;
    private final ApprovalWorkflowLevelRepository approvalWorkflowLevelRepository;
    private final GroupBrandRepository groupBrandRepository;
    private final UserRepository userRepository;

    // ─── LIST ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> findAll(String groupBrandId, int page, int pageSize) {
        Specification<ApprovalWorkflow> spec = Specification.where(null);

        if (groupBrandId != null && !groupBrandId.isBlank()) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("groupBrandId"), Long.parseLong(groupBrandId)));
        }

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ApprovalWorkflow> pageResult = approvalWorkflowRepository.findAll(spec, pageable);

        List<ApprovalWorkflowResponse> data = pageResult.getContent().stream()
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
    public ApprovalWorkflowResponse findById(Long id) {
        ApprovalWorkflow workflow = approvalWorkflowRepository.findById(id)
            .orElseThrow(() -> new ApprovalWorkflowNotFoundException(id.toString()));
        return toResponse(workflow);
    }

    // ─── GET BY GROUP BRAND ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ApprovalWorkflowResponse> findByGroupBrand(String groupBrandId) {
        List<ApprovalWorkflow> workflows = approvalWorkflowRepository
            .findByGroupBrandId(Long.parseLong(groupBrandId));
        return workflows.stream().map(this::toResponse).toList();
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    public ApprovalWorkflowResponse create(CreateApprovalWorkflowRequest dto, Long userId) {
        User creator = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        GroupBrand groupBrand = groupBrandRepository.findById(Long.parseLong(dto.groupBrandId()))
            .orElseThrow(() -> new IllegalArgumentException("GroupBrand not found: " + dto.groupBrandId()));

        ApprovalWorkflow workflow = ApprovalWorkflow.builder()
            .groupBrandId(Long.parseLong(dto.groupBrandId()))
            .workflowName(dto.workflowName())
            .createdBy(userId)
            .createdAt(Instant.now())
            .build();
        workflow = approvalWorkflowRepository.save(workflow);

        // Create levels if provided
        if (dto.levels() != null && !dto.levels().isEmpty()) {
            int order = 1;
            for (CreateApprovalWorkflowRequest.ApprovalWorkflowLevelRequest levelReq : dto.levels()) {
                ApprovalWorkflowLevel level = ApprovalWorkflowLevel.builder()
                    .approvalWorkflowId(workflow.getId())
                    .levelOrder(order++)
                    .levelName(levelReq.levelName())
                    .approverUserId(levelReq.approverUserId() != null ? Long.parseLong(levelReq.approverUserId()) : null)
                    .isRequired(levelReq.isRequired() != null ? levelReq.isRequired() : true)
                    .createdBy(userId)
                    .createdAt(Instant.now())
                    .build();
                approvalWorkflowLevelRepository.save(level);
            }
        }

        return findById(workflow.getId());
    }

    // ─── ADD LEVEL ───────────────────────────────────────────────────────────

    public ApprovalWorkflowResponse addLevel(Long workflowId, UpdateApprovalWorkflowLevelRequest dto, Long userId) {
        ApprovalWorkflow workflow = approvalWorkflowRepository.findById(workflowId)
            .orElseThrow(() -> new ApprovalWorkflowNotFoundException(workflowId.toString()));

        // Get max level order
        Integer maxOrder = approvalWorkflowLevelRepository.findMaxLevelOrderByWorkflowId(workflowId);
        int newOrder = (maxOrder != null ? maxOrder : 0) + 1;

        ApprovalWorkflowLevel level = ApprovalWorkflowLevel.builder()
            .approvalWorkflowId(workflowId)
            .levelOrder(newOrder)
            .levelName(dto.levelName())
            .approverUserId(dto.approverUserId() != null ? Long.parseLong(dto.approverUserId()) : null)
            .isRequired(dto.isRequired() != null ? dto.isRequired() : true)
            .createdBy(userId)
            .createdAt(Instant.now())
            .build();
        approvalWorkflowLevelRepository.save(level);

        return findById(workflowId);
    }

    // ─── UPDATE LEVEL ─────────────────────────────────────────────────────────

    public ApprovalWorkflowResponse updateLevel(Long levelId, UpdateApprovalWorkflowLevelRequest dto, Long userId) {
        ApprovalWorkflowLevel level = approvalWorkflowLevelRepository.findById(levelId)
            .orElseThrow(() -> new ApprovalWorkflowLevelNotFoundException(levelId.toString()));

        if (dto.levelName() != null) level.setLevelName(dto.levelName());
        if (dto.approverUserId() != null) level.setApproverUserId(Long.parseLong(dto.approverUserId()));
        if (dto.isRequired() != null) level.setIsRequired(dto.isRequired());

        level.setUpdatedBy(userId);
        level.setUpdatedAt(Instant.now());
        approvalWorkflowLevelRepository.save(level);

        return findById(level.getApprovalWorkflowId());
    }

    // ─── REMOVE LEVEL ─────────────────────────────────────────────────────────

    public ApprovalWorkflowResponse removeLevel(Long levelId) {
        ApprovalWorkflowLevel level = approvalWorkflowLevelRepository.findById(levelId)
            .orElseThrow(() -> new ApprovalWorkflowLevelNotFoundException(levelId.toString()));

        Long workflowId = level.getApprovalWorkflowId();
        approvalWorkflowLevelRepository.delete(level);

        // Reorder remaining levels
        List<ApprovalWorkflowLevel> remaining = approvalWorkflowLevelRepository.findByApprovalWorkflowIdOrderByLevelOrder(workflowId);
        int order = 1;
        for (ApprovalWorkflowLevel l : remaining) {
            l.setLevelOrder(order++);
        }
        approvalWorkflowLevelRepository.saveAll(remaining);

        return findById(workflowId);
    }

    // ─── REMOVE WORKFLOW ──────────────────────────────────────────────────────

    public void remove(Long workflowId) {
        ApprovalWorkflow workflow = approvalWorkflowRepository.findById(workflowId)
            .orElseThrow(() -> new ApprovalWorkflowNotFoundException(workflowId.toString()));

        // Delete all levels first
        List<ApprovalWorkflowLevel> levels = approvalWorkflowLevelRepository.findByApprovalWorkflowIdOrderByLevelOrder(workflowId);
        approvalWorkflowLevelRepository.deleteAll(levels);

        approvalWorkflowRepository.delete(workflow);
    }

    // ─── REORDER LEVELS ────────────────────────────────────────────────────────

    public ApprovalWorkflowResponse reorderLevels(Long workflowId, List<String> levelIds) {
        ApprovalWorkflow workflow = approvalWorkflowRepository.findById(workflowId)
            .orElseThrow(() -> new ApprovalWorkflowNotFoundException(workflowId.toString()));

        if (levelIds == null || levelIds.isEmpty()) {
            throw new IllegalArgumentException("levelIds cannot be empty");
        }

        int order = 1;
        for (String levelIdStr : levelIds) {
            Long levelId = Long.parseLong(levelIdStr);
            ApprovalWorkflowLevel level = approvalWorkflowLevelRepository.findById(levelId)
                .orElseThrow(() -> new ApprovalWorkflowLevelNotFoundException(levelIdStr));
            if (!level.getApprovalWorkflowId().equals(workflowId)) {
                throw new IllegalArgumentException("Level " + levelId + " does not belong to workflow " + workflowId);
            }
            level.setLevelOrder(order++);
            level.setUpdatedAt(Instant.now());
            approvalWorkflowLevelRepository.save(level);
        }

        return findById(workflowId);
    }

    // ─── MAPPING ──────────────────────────────────────────────────────────────

    private ApprovalWorkflowResponse toResponse(ApprovalWorkflow workflow) {
        List<ApprovalWorkflowLevel> levels = approvalWorkflowLevelRepository
            .findByApprovalWorkflowIdOrderByLevelOrder(workflow.getId());

        List<ApprovalWorkflowResponse.ApprovalWorkflowLevelResponse> levelResponses = new ArrayList<>();
        for (ApprovalWorkflowLevel level : levels) {
            String approverUserName = null;
            if (level.getApproverUserId() != null) {
                User approver = userRepository.findById(level.getApproverUserId()).orElse(null);
                if (approver != null) {
                    approverUserName = approver.getName();
                }
            }

            levelResponses.add(ApprovalWorkflowResponse.ApprovalWorkflowLevelResponse.builder()
                .id(level.getId())
                .levelOrder(level.getLevelOrder())
                .levelName(level.getLevelName())
                .approverUserId(level.getApproverUserId())
                .approverUserName(approverUserName)
                .isRequired(level.getIsRequired())
                .createdAt(level.getCreatedAt())
                .updatedAt(level.getUpdatedAt())
                .build());
        }

        String groupBrandName = null;
        if (workflow.getGroupBrandId() != null) {
            GroupBrand gb = groupBrandRepository.findById(workflow.getGroupBrandId()).orElse(null);
            if (gb != null) {
                groupBrandName = gb.getName();
            }
        }

        ApprovalWorkflowResponse.CreatorSummary creatorSummary = null;
        if (workflow.getCreatedBy() != null) {
            User creator = userRepository.findById(workflow.getCreatedBy()).orElse(null);
            if (creator != null) {
                creatorSummary = ApprovalWorkflowResponse.CreatorSummary.builder()
                    .id(creator.getId())
                    .name(creator.getName())
                    .email(creator.getEmail())
                    .build();
            }
        }

        return ApprovalWorkflowResponse.builder()
            .id(workflow.getId())
            .groupBrandId(workflow.getGroupBrandId())
            .groupBrandName(groupBrandName)
            .workflowName(workflow.getWorkflowName())
            .createdAt(workflow.getCreatedAt())
            .updatedAt(workflow.getUpdatedAt())
            .creator(creatorSummary)
            .levels(levelResponses)
            .build();
    }
}
