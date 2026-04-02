package com.vieterp.otb.approvalworkflow;

import com.vieterp.otb.approvalworkflow.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/approval-workflows")
@RequiredArgsConstructor
@Tag(name = "Approval Workflows", description = "Approval workflow management")
public class ApprovalWorkflowController {

    private final ApprovalWorkflowService approvalWorkflowService;

    // ─── LIST ─────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List approval workflows with filters and pagination")
    public ResponseEntity<Map<String, Object>> findAll(
            @Parameter(description = "Group brand ID filter") @RequestParam(name = "groupBrandId", required = false) String groupBrandId,
            @Parameter(description = "Page number (default 1)") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "Page size (default 20)") @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(approvalWorkflowService.findAll(groupBrandId, page, pageSize));
    }

    // ─── GET ONE ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get approval workflow by ID with levels")
    public ResponseEntity<ApprovalWorkflowResponse> findOne(@Parameter @PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(approvalWorkflowService.findById(id));
    }

    // ─── GET BY GROUP BRAND ────────────────────────────────────────────────────

    @GetMapping("/group-brand/{groupBrandId}")
    @Operation(summary = "Get all approval workflows for a group brand")
    public ResponseEntity<java.util.List<ApprovalWorkflowResponse>> findByGroupBrand(
            @Parameter @PathVariable(name = "groupBrandId") String groupBrandId) {
        return ResponseEntity.ok(approvalWorkflowService.findByGroupBrand(groupBrandId));
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create new approval workflow")
    public ResponseEntity<ApprovalWorkflowResponse> create(
            @Valid @RequestBody CreateApprovalWorkflowRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        ApprovalWorkflowResponse response = approvalWorkflowService.create(dto, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    // ─── ADD LEVEL ─────────────────────────────────────────────────────────────

    @PostMapping("/{id}/levels")
    @Operation(summary = "Add a new level to an approval workflow")
    public ResponseEntity<ApprovalWorkflowResponse> addLevel(
            @Parameter @PathVariable(name = "id") Long id,
            @Valid @RequestBody UpdateApprovalWorkflowLevelRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(approvalWorkflowService.addLevel(id, dto, userId));
    }

    // ─── UPDATE LEVEL ─────────────────────────────────────────────────────────

    @PutMapping("/levels/{levelId}")
    @Operation(summary = "Update an approval workflow level")
    public ResponseEntity<ApprovalWorkflowResponse> updateLevel(
            @Parameter @PathVariable(name = "levelId") Long levelId,
            @Valid @RequestBody UpdateApprovalWorkflowLevelRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(approvalWorkflowService.updateLevel(levelId, dto, userId));
    }

    // ─── REMOVE LEVEL ─────────────────────────────────────────────────────────

    @DeleteMapping("/levels/{levelId}")
    @Operation(summary = "Remove a level from an approval workflow")
    public ResponseEntity<Map<String, Object>> removeLevel(@Parameter @PathVariable(name = "levelId") Long levelId) {
        approvalWorkflowService.removeLevel(levelId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Level removed"));
    }

    // ─── REORDER LEVELS ───────────────────────────────────────────────────────

    @PatchMapping("/{id}/reorder-levels")
    @Operation(summary = "Reorder levels in an approval workflow")
    public ResponseEntity<ApprovalWorkflowResponse> reorderLevels(
            @Parameter @PathVariable(name = "id") Long id,
            @RequestBody java.util.List<String> levelIds) {
        return ResponseEntity.ok(approvalWorkflowService.reorderLevels(id, levelIds));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an approval workflow and all its levels")
    public ResponseEntity<Map<String, Object>> remove(@Parameter @PathVariable(name = "id") Long id) {
        approvalWorkflowService.remove(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Approval workflow deleted"));
    }
}
