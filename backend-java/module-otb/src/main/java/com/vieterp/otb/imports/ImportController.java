package com.vieterp.otb.imports;

import com.vieterp.otb.imports.ImportService;
import com.vieterp.otb.imports.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/import")
@RequiredArgsConstructor
@Tag(name = "Import", description = "Bulk data import (CSV/Excel) management")
public class ImportController {

    private final ImportService importService;

    // ─── PROCESS BATCH ─────────────────────────────────────────────────────────

    @PostMapping("/batch")
    @Operation(summary = "Process an import batch")
    public ResponseEntity<Map<String, Object>> processBatch(
            @Parameter(description = "Import batch data") @RequestBody ImportBatchDto dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(importService.processBatch(dto, userId));
    }

    // ─── QUERY DATA ────────────────────────────────────────────────────────────

    @GetMapping("/data")
    @Operation(summary = "Query imported data with filters and pagination")
    public ResponseEntity<Map<String, Object>> queryData(
            @Parameter(description = "Target type filter") @RequestParam(name = "target", required = false) ImportTargetEnum target,
            @Parameter(description = "Page number (default 1)") @RequestParam(name = "page", defaultValue = "1") Integer page,
            @Parameter(description = "Page size (default 20)") @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize,
            @Parameter(description = "Search term") @RequestParam(name = "search", required = false) String search,
            @Parameter(description = "Sort field") @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort order (ASC/DESC)") @RequestParam(name = "sortOrder", defaultValue = "DESC") String sortOrder) {
        ImportQueryDto query = ImportQueryDto.builder()
            .target(target)
            .page(page)
            .pageSize(pageSize)
            .search(search)
            .sortBy(sortBy)
            .sortOrder(sortOrder)
            .build();
        return ResponseEntity.ok(importService.queryData(query));
    }

    // ─── GET STATS ─────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    @Operation(summary = "Get import statistics for a target")
    public ResponseEntity<Map<String, Object>> getStats(
            @Parameter(description = "Target type") @RequestParam(name = "target", required = false) String target) {
        return ResponseEntity.ok(importService.getStats(target));
    }

    // ─── GET ALL STATS ─────────────────────────────────────────────────────────

    @GetMapping("/all-stats")
    @Operation(summary = "Get import statistics for all targets")
    public ResponseEntity<Map<String, Object>> getAllTargetStats() {
        return ResponseEntity.ok(importService.getAllTargetStats());
    }

    // ─── WSSI ANALYTICS ────────────────────────────────────────────────────────

    @GetMapping("/wssi/analytics")
    @Operation(summary = "Get WSSI analytics from imported data")
    public ResponseEntity<Map<String, Object>> getWssiAnalytics() {
        return ResponseEntity.ok(importService.getWssiAnalytics());
    }

    // ─── APPLY IMPORTED DATA ───────────────────────────────────────────────────

    @PostMapping("/apply")
    @Operation(summary = "Apply imported data to target entity")
    public ResponseEntity<Map<String, Object>> applyImportedData(
            @Parameter(description = "Target type") @RequestParam(name = "target") String target,
            @Parameter(description = "Session ID (optional - uses latest if not provided)") @RequestParam(name = "sessionId", required = false) String sessionId) {
        ImportService.ApplyResult result = importService.applyImportedData(target, sessionId);
        Map<String, Object> response = Map.of(
            "success", result.success(),
            "failed", result.failed(),
            "errors", result.errors()
        );
        return ResponseEntity.ok(response);
    }

    // ─── DELETE DATA ───────────────────────────────────────────────────────────

    @DeleteMapping("/data")
    @Operation(summary = "Delete imported data")
    public ResponseEntity<Map<String, Object>> deleteData(
            @Parameter(description = "Delete options") @RequestBody ImportDeleteDto dto) {
        if (Boolean.TRUE.equals(dto.getClearAll())) {
            Long count = importService.clearAll(dto.getTarget().name());
            return ResponseEntity.ok(Map.of("success", true, "cleared", count));
        } else {
            Long deletedId = importService.deleteSession(
                dto.getTarget() != null ? dto.getTarget().name() : null,
                dto.getSessionId()
            );
            return ResponseEntity.ok(Map.of("success", true, "deletedSessionId", deletedId));
        }
    }

    // ─── DELETE SESSION ───────────────────────────────────────────────────────

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "Delete import session and all its records")
    public ResponseEntity<Map<String, Object>> deleteSession(
            @Parameter(description = "Target type") @RequestParam(name = "target", required = false) String target,
            @Parameter(description = "Session ID") @PathVariable(name = "sessionId") String sessionId) {
        Long deletedId = importService.deleteSession(target, sessionId);
        return ResponseEntity.ok(Map.of("success", true, "deletedSessionId", deletedId));
    }

    // ─── CLEAR TARGET ─────────────────────────────────────────────────────────

    @DeleteMapping("/clear/{target}")
    @Operation(summary = "Clear all import sessions and records for a target")
    public ResponseEntity<Map<String, Object>> clearTarget(
            @Parameter(description = "Target type") @PathVariable(name = "target") String target) {
        Long count = importService.clearAll(target);
        return ResponseEntity.ok(Map.of("success", true, "clearedSessions", count));
    }
}
