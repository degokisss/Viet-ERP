package com.vieterp.otb.ticket;

import com.vieterp.otb.ticket.dto.*;
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
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Ticket management and approval workflow")
public class TicketController {

    private final TicketService ticketService;

    // ─── VALIDATE BUDGET READINESS ─────────────────────────────────────────────

    @GetMapping("/validate-budget/{budgetId}")
    @Operation(summary = "Validate if budget is ready for ticket creation (4-step validation)")
    public ResponseEntity<TicketValidationResponse> validateBudgetReadiness(
            @Parameter @PathVariable(name = "budgetId") String budgetId) {
        return ResponseEntity.ok(ticketService.validateBudgetReadiness(budgetId));
    }

    // ─── LIST ─────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List tickets with filters and pagination")
    public ResponseEntity<Map<String, Object>> findAll(
            @Parameter(description = "Status filter") @RequestParam(name = "status", required = false) String status,
            @Parameter(description = "Budget ID filter") @RequestParam(name = "budgetId", required = false) String budgetId,
            @Parameter(description = "Season group ID filter") @RequestParam(name = "seasonGroupId", required = false) String seasonGroupId,
            @Parameter(description = "Season ID filter") @RequestParam(name = "seasonId", required = false) String seasonId,
            @Parameter(description = "Page number (default 1)") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "Page size (default 20)") @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(ticketService.findAll(status, budgetId, seasonGroupId, seasonId, page, pageSize));
    }

    // ─── STATISTICS ───────────────────────────────────────────────────────────

    @GetMapping("/statistics")
    @Operation(summary = "Get ticket statistics")
    public ResponseEntity<TicketStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(ticketService.getStatistics());
    }

    // ─── GET ONE ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket by ID with approval history")
    public ResponseEntity<TicketResponse> findOne(@Parameter @PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(ticketService.findById(id));
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create new ticket and snapshot copies of all finalized budget data")
    public ResponseEntity<TicketResponse> create(
            @Valid @RequestBody CreateTicketRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        TicketResponse response = ticketService.create(dto, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    // ─── PROCESS APPROVAL ─────────────────────────────────────────────────────

    @PostMapping("/{id}/process-approval")
    @Operation(summary = "Process approval decision for a ticket")
    public ResponseEntity<ProcessApprovalResponse> processApproval(
            @Parameter @PathVariable(name = "id") Long id,
            @Valid @RequestBody ProcessApprovalRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(ticketService.processApproval(id, dto, userId));
    }

    // ─── GET APPROVAL HISTORY ─────────────────────────────────────────────────

    @GetMapping("/{id}/approval-history")
    @Operation(summary = "Get approval history for a ticket")
    public ResponseEntity<java.util.List<TicketResponse.TicketApprovalLogResponse>> getApprovalHistory(
            @Parameter @PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(ticketService.getApprovalHistory(id));
    }
}
