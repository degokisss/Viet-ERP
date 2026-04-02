package com.vieterp.otb.planning;

import com.vieterp.otb.planning.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/planning")
@RequiredArgsConstructor
@Tag(name = "Planning", description = "OTB Planning management")
public class PlanningController {

    private final PlanningService planningService;

    // ─── LIST ─────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List planning headers with filters and pagination")
    public ResponseEntity<Map<String, Object>> findAll(
            @Parameter(description = "Page number (default 1)") @RequestParam(name = "page", required = false) Integer page,
            @Parameter(description = "Page size (default 20)") @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @Parameter(description = "Status filter (DRAFT, SUBMITTED, APPROVED, REJECTED)") @RequestParam(name = "status", required = false) String status,
            @Parameter(description = "Budget ID filter (reserved)") @RequestParam(name = "budgetId", required = false) String budgetId,
            @Parameter(description = "Brand ID filter (via allocate_header)") @RequestParam(name = "brandId", required = false) String brandId,
            @Parameter(description = "Allocate header ID filter") @RequestParam(name = "allocateHeaderId", required = false) String allocateHeaderId) {
        return ResponseEntity.ok(planningService.findAll(page, pageSize, status, budgetId, brandId, allocateHeaderId));
    }

    // ─── FILTER OPTIONS ────────────────────────────────────────────────────────

    @GetMapping("/filter-options/categories")
    @Operation(summary = "Get Gender → Category → SubCategory hierarchy for Planning Detail filter dropdowns")
    public ResponseEntity<PlanningFilterOptionsResponse> getCategoryFilterOptions(
            @Parameter(description = "Filter by gender ID (cascading)") @RequestParam(name = "genderId", required = false) String genderId,
            @Parameter(description = "Filter by category ID (cascading to sub-categories)") @RequestParam(name = "categoryId", required = false) String categoryId) {
        return ResponseEntity.ok(planningService.getCategoryFilterOptions(genderId, categoryId));
    }

    // ─── HISTORICAL ───────────────────────────────────────────────────────────

    @GetMapping("/historical")
    @Operation(summary = "Get historical planning data for year/season/brand comparison")
    public ResponseEntity<PlanningResponse> findHistorical(
            @Parameter(description = "Fiscal year", required = true) @RequestParam(name = "fiscalYear") Integer fiscalYear,
            @Parameter(description = "Season group name", required = true) @RequestParam(name = "seasonGroupName") String seasonGroupName,
            @Parameter(description = "Season name", required = true) @RequestParam(name = "seasonName") String seasonName,
            @Parameter(description = "Brand ID", required = true) @RequestParam(name = "brandId") String brandId) {
        PlanningResponse result = planningService.findHistorical(fiscalYear, seasonGroupName, seasonName, brandId);
        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    // ─── GET ONE ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get planning header with all details (collections, genders, categories)")
    public ResponseEntity<PlanningResponse> findOne(@Parameter @PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(planningService.findOne(id));
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create new planning header with details")
    public ResponseEntity<PlanningResponse> create(
            @Valid @RequestBody CreatePlanningRequest dto,
            @Parameter(description = "User ID") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        PlanningResponse response = planningService.create(dto, userId);
        return ResponseEntity.ok(response);
    }

    // ─── COPY FROM EXISTING ───────────────────────────────────────────────────

    @PostMapping("/{id}/copy")
    @Operation(summary = "Create new version by copying an existing one")
    public ResponseEntity<PlanningResponse> createFromVersion(
            @Parameter @PathVariable(name = "id") Long id,
            @Parameter(description = "User ID") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(planningService.createFromVersion(id, userId));
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Update planning header details")
    public ResponseEntity<PlanningResponse> update(
            @Parameter @PathVariable(name = "id") Long id,
            @Valid @RequestBody UpdatePlanningRequest dto,
            @Parameter(description = "User ID") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(planningService.update(id, dto, userId));
    }

    // ─── SUBMIT ───────────────────────────────────────────────────────────────

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit planning for approval (DRAFT → SUBMITTED)")
    public ResponseEntity<Map<String, Object>> submit(
            @Parameter @PathVariable(name = "id") Long id,
            @Parameter(description = "User ID") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        planningService.submit(id, userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Planning submitted"));
    }

    // ─── APPROVE BY LEVEL ─────────────────────────────────────────────────────

    @PostMapping("/{id}/approve/{level}")
    @Operation(summary = "Approve or reject planning by level (action: APPROVED | REJECTED)")
    public ResponseEntity<Map<String, Object>> approveByLevel(
            @Parameter @PathVariable(name = "id") Long id,
            @Parameter @PathVariable(name = "level") String level,
            @Parameter(description = "Action: APPROVED or REJECTED") @RequestParam(name = "action") String action,
            @Parameter(description = "Comment") @RequestParam(name = "comment", required = false) String comment,
            @Parameter(description = "User ID") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        planningService.approveByLevel(id, level, action, comment, userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Planning " + action.toLowerCase()));
    }

    // ─── FINALIZE ─────────────────────────────────────────────────────────────

    @PostMapping("/{id}/final")
    @Operation(summary = "Mark planning version as final")
    public ResponseEntity<Map<String, Object>> finalize(
            @Parameter @PathVariable(name = "id") Long id,
            @Parameter(description = "User ID") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        planningService.finalize(id, userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Planning finalized"));
    }

    // ─── UPDATE DETAIL ────────────────────────────────────────────────────────

    @PatchMapping("/{id}/details/{detailId}")
    @Operation(summary = "Update a single planning detail row")
    public ResponseEntity<Object> updateDetail(
            @Parameter @PathVariable(name = "id") Long id,
            @Parameter @PathVariable(name = "detailId") Long detailId,
            @RequestBody Object dto,
            @Parameter(description = "User ID") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(planningService.updateDetail(id, detailId, dto, userId));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete planning header")
    public ResponseEntity<Map<String, Object>> remove(@Parameter @PathVariable(name = "id") Long id) {
        planningService.remove(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Planning header deleted"));
    }
}
