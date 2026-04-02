package com.vieterp.otb.budget;

import com.vieterp.otb.budget.domain.dto.*;
import com.vieterp.otb.budget.exception.BudgetNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets", description = "Budget management and allocation")
public class BudgetController {

    private final BudgetService budgetService;

    // ─── LIST ─────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List budgets with filters and pagination")
    public ResponseEntity<Map<String, Object>> findAll(
            @Parameter(description = "Fiscal year filter") @RequestParam(name = "fiscalYear", required = false) Integer fiscalYear,
            @Parameter(description = "Status filter (DRAFT, SUBMITTED, APPROVED, REJECTED)") @RequestParam(name = "status", required = false) String status,
            @Parameter(description = "Page number (default 1)") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "Page size (default 20)") @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(budgetService.findAll(fiscalYear, status, page, pageSize));
    }

    // ─── STATISTICS ───────────────────────────────────────────────────────────

    @GetMapping("/statistics")
    @Operation(summary = "Get budget statistics")
    public ResponseEntity<BudgetStatisticsResponse> getStatistics(
            @Parameter(description = "Fiscal year filter") @RequestParam(name = "fiscalYear", required = false) Integer fiscalYear) {
        return ResponseEntity.ok(budgetService.getStatistics(fiscalYear));
    }

    // ─── GET ONE ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get budget by ID with allocations")
    public ResponseEntity<BudgetResponse> findOne(@Parameter @PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(budgetService.findById(id));
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create new budget")
    public ResponseEntity<BudgetResponse> create(
            @Valid @RequestBody CreateBudgetRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        BudgetResponse response = budgetService.create(dto, userId);
        return ResponseEntity.created(URI.create("/budgets/" + response.id())).body(response);
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Update draft budget")
    public ResponseEntity<BudgetResponse> update(
            @Parameter @PathVariable(name = "id") Long id,
            @Valid @RequestBody UpdateBudgetRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(budgetService.update(id, dto, userId));
    }

    // ─── CREATE ALLOCATION HEADER ──────────────────────────────────────────────

    @PostMapping("/{id}/allocations")
    @Operation(summary = "Create new allocation version for a budget")
    public ResponseEntity<BudgetResponse> createAllocation(
            @Parameter @PathVariable(name = "id") Long id,
            @Valid @RequestBody CreateAllocateRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(budgetService.createAllocateHeader(id, dto, userId));
    }

    // ─── UPDATE ALLOCATION HEADER ──────────────────────────────────────────────

    @PutMapping("/allocations/{headerId}")
    @Operation(summary = "Update allocation header details")
    public ResponseEntity<BudgetResponse> updateAllocation(
            @Parameter @PathVariable(name = "headerId") Long headerId,
            @Valid @RequestBody UpdateAllocateRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(budgetService.updateAllocateHeader(headerId, dto, userId));
    }

    // ─── SET FINAL ALLOCATE VERSION ────────────────────────────────────────────

    @PatchMapping("/allocations/{headerId}/set-final")
    @Operation(summary = "Mark allocation version as final")
    public ResponseEntity<BudgetResponse> setFinalVersion(@Parameter @PathVariable(name = "headerId") Long headerId) {
        return ResponseEntity.ok(budgetService.setFinalVersion(headerId));
    }

    // ─── SUBMIT ────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit budget for approval (DRAFT → SUBMITTED)")
    public ResponseEntity<Map<String, Object>> submit(
            @Parameter @PathVariable(name = "id") Long id,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        budgetService.submit(id, userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Budget submitted"));
    }

    // ─── APPROVE ───────────────────────────────────────────────────────────────

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve budget (SUBMITTED → APPROVED)")
    public ResponseEntity<Map<String, Object>> approve(
            @Parameter @PathVariable(name = "id") Long id,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        budgetService.approve(id, userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Budget approved"));
    }

    // ─── REJECT ────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject budget (SUBMITTED → REJECTED)")
    public ResponseEntity<Map<String, Object>> reject(
            @Parameter @PathVariable(name = "id") Long id,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        budgetService.reject(id, userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Budget rejected"));
    }

    // ─── ARCHIVE ───────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive approved budget (APPROVED → ARCHIVED)")
    public ResponseEntity<Map<String, Object>> archive(@Parameter @PathVariable(name = "id") Long id) {
        budgetService.archive(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Budget archived"));
    }

    // ─── DELETE ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete draft budget")
    public ResponseEntity<Map<String, Object>> remove(@Parameter @PathVariable(name = "id") Long id) {
        budgetService.remove(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Budget deleted"));
    }
}