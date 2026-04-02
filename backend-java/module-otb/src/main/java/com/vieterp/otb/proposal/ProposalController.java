package com.vieterp.otb.proposal;

import com.vieterp.otb.proposal.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/proposals")
@RequiredArgsConstructor
@Tag(name = "Proposals", description = "SKU Proposal management")
public class ProposalController {

    private final ProposalService proposalService;

    // ─── LIST ─────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List proposals with filters and pagination")
    public ResponseEntity<Map<String, Object>> findAll(
            @Parameter(description = "Status filter (DRAFT, SUBMITTED, APPROVED, REJECTED)") @RequestParam(name = "status", required = false) String status,
            @Parameter(description = "Page number (default 1)") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "Page size (default 20)") @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(proposalService.findAll(status, page, pageSize));
    }

    // ─── STATISTICS ───────────────────────────────────────────────────────────

    @GetMapping("/statistics")
    @Operation(summary = "Get proposal statistics")
    public ResponseEntity<ProposalStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(proposalService.getStatistics());
    }

    // ─── GET ONE ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get proposal by ID with all nested data")
    public ResponseEntity<ProposalResponse> findOne(@Parameter @PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(proposalService.findById(id));
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create new proposal")
    public ResponseEntity<ProposalResponse> create(
            @Valid @RequestBody CreateProposalRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        ProposalResponse response = proposalService.create(dto, userId);
        return ResponseEntity.created(URI.create("/proposals/" + response.id())).body(response);
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Update draft proposal")
    public ResponseEntity<ProposalResponse> update(
            @Parameter @PathVariable(name = "id") Long id,
            @Valid @RequestBody UpdateProposalRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(proposalService.update(id, dto, userId));
    }

    // ─── SAVE FULL PROPOSAL ──────────────────────────────────────────────────

    @PutMapping("/{id}/full")
    @Operation(summary = "Save full proposal (replace all items, allocations, sizings in one transaction)")
    public ResponseEntity<ProposalResponse> saveFullProposal(
            @Parameter @PathVariable(name = "id") Long id,
            @Valid @RequestBody SaveFullProposalRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(proposalService.saveFullProposal(id, dto, userId));
    }

    // ─── COPY PROPOSAL ───────────────────────────────────────────────────────

    @PostMapping("/{id}/copy")
    @Operation(summary = "Copy an existing proposal as a new draft")
    public ResponseEntity<ProposalResponse> copyProposal(
            @Parameter @PathVariable(name = "id") Long id,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        ProposalResponse response = proposalService.copyProposal(id, userId);
        return ResponseEntity.created(URI.create("/proposals/" + response.id())).body(response);
    }

    // ─── ADD PRODUCT ─────────────────────────────────────────────────────────

    @PostMapping("/{id}/products")
    @Operation(summary = "Add a product to proposal")
    public ResponseEntity<ProposalResponse> addProduct(
            @Parameter @PathVariable(name = "id") Long id,
            @Valid @RequestBody CreateSKUProposalRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(proposalService.addProduct(id, dto, userId));
    }

    // ─── BULK ADD PRODUCTS ───────────────────────────────────────────────────

    @PostMapping("/{id}/products/bulk")
    @Operation(summary = "Bulk add products to proposal")
    public ResponseEntity<ProposalResponse> bulkAddProducts(
            @Parameter @PathVariable(name = "id") Long id,
            @Valid @RequestBody BulkAddProductsRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(proposalService.bulkAddProducts(id, dto, userId));
    }

    // ─── UPDATE PROPOSAL ITEM ─────────────────────────────────────────────────

    @PutMapping("/products/{itemId}")
    @Operation(summary = "Update a proposal item")
    public ResponseEntity<ProposalResponse> updateProposal(
            @Parameter @PathVariable(name = "itemId") Long itemId,
            @Valid @RequestBody UpdateSKUProposalRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(proposalService.updateProposal(itemId, dto, userId));
    }

    // ─── REMOVE PROPOSAL ITEM ─────────────────────────────────────────────────

    @DeleteMapping("/products/{itemId}")
    @Operation(summary = "Remove a proposal item")
    public ResponseEntity<ProposalResponse> removeProposal(
            @Parameter @PathVariable(name = "itemId") Long itemId,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(proposalService.removeProposal(itemId, userId));
    }

    // ─── CREATE ALLOCATIONS ───────────────────────────────────────────────────

    @PostMapping("/{id}/allocations")
    @Operation(summary = "Create store allocations for a proposal")
    public ResponseEntity<ProposalResponse> createAllocations(
            @Parameter @PathVariable(name = "id") Long id,
            @Valid @RequestBody CreateAllocateRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(proposalService.createAllocations(id, dto, userId));
    }

    // ─── GET STORE ALLOCATIONS ───────────────────────────────────────────────

    @GetMapping("/{id}/allocations")
    @Operation(summary = "Get store allocations for a proposal")
    public ResponseEntity<List<ProposalResponse.AllocateSummary>> getStoreAllocations(@Parameter @PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(proposalService.getStoreAllocations(id));
    }

    // ─── UPDATE ALLOCATION ───────────────────────────────────────────────────

    @PutMapping("/allocations/{allocationId}")
    @Operation(summary = "Update a store allocation")
    public ResponseEntity<ProposalResponse> updateAllocation(
            @Parameter @PathVariable(name = "allocationId") Long allocationId,
            @Valid @RequestBody UpdateAllocateRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(proposalService.updateAllocation(allocationId, dto, userId));
    }

    // ─── DELETE ALLOCATION ───────────────────────────────────────────────────

    @DeleteMapping("/allocations/{allocationId}")
    @Operation(summary = "Delete a store allocation")
    public ResponseEntity<Map<String, Object>> deleteAllocation(@Parameter @PathVariable(name = "allocationId") Long allocationId) {
        proposalService.deleteAllocation(allocationId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Allocation deleted"));
    }

    // ─── CREATE SIZING HEADER ───────────────────────────────────────────────

    @PostMapping("/{id}/sizing-headers")
    @Operation(summary = "Create a sizing header for a proposal (max 3)")
    public ResponseEntity<ProposalResponse> createSizingHeader(
            @Parameter @PathVariable(name = "id") Long id,
            @Valid @RequestBody CreateSizingHeaderRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(proposalService.createSizingHeader(id, dto, userId));
    }

    // ─── GET SIZING HEADERS ─────────────────────────────────────────────────

    @GetMapping("/{id}/sizing-headers")
    @Operation(summary = "Get sizing headers for a proposal")
    public ResponseEntity<List<ProposalResponse.SizingHeaderSummary>> getSizingHeaders(@Parameter @PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(proposalService.getSizingHeaders(id));
    }

    // ─── UPDATE SIZING HEADER ───────────────────────────────────────────────

    @PutMapping("/sizing-headers/{headerId}")
    @Operation(summary = "Update a sizing header")
    public ResponseEntity<ProposalResponse> updateSizingHeader(
            @Parameter @PathVariable(name = "headerId") Long headerId,
            @Valid @RequestBody UpdateSizingHeaderRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(proposalService.updateSizingHeader(headerId, dto, userId));
    }

    // ─── DELETE SIZING HEADER ───────────────────────────────────────────────

    @DeleteMapping("/sizing-headers/{headerId}")
    @Operation(summary = "Delete a sizing header (min 1 must remain)")
    public ResponseEntity<ProposalResponse> deleteSizingHeader(
            @Parameter @PathVariable(name = "headerId") Long headerId,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(proposalService.deleteSizingHeader(headerId, userId));
    }

    // ─── CREATE SIZINGS ─────────────────────────────────────────────────────

    @PostMapping("/sizing-headers/{headerId}/sizings")
    @Operation(summary = "Create sizing rows for a sizing header")
    public ResponseEntity<ProposalResponse> createSizings(
            @Parameter @PathVariable(name = "headerId") Long headerId,
            @Valid @RequestBody CreateSizingRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(proposalService.createSizings(headerId, dto, userId));
    }

    // ─── GET SIZINGS ────────────────────────────────────────────────────────

    @GetMapping("/sizing-headers/{headerId}/sizings")
    @Operation(summary = "Get sizing rows for a sizing header")
    public ResponseEntity<List<ProposalResponse.SizingSummary>> getSizings(@Parameter @PathVariable(name = "headerId") Long headerId) {
        return ResponseEntity.ok(proposalService.getSizings(headerId));
    }

    // ─── UPDATE SIZING ─────────────────────────────────────────────────────

    @PutMapping("/sizings/{sizingId}")
    @Operation(summary = "Update a sizing row")
    public ResponseEntity<ProposalResponse> updateSizing(
            @Parameter @PathVariable(name = "sizingId") Long sizingId,
            @Valid @RequestBody UpdateSizingRequest dto,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(proposalService.updateSizing(sizingId, dto, userId));
    }

    // ─── DELETE SIZING ─────────────────────────────────────────────────────

    @DeleteMapping("/sizings/{sizingId}")
    @Operation(summary = "Delete a sizing row")
    public ResponseEntity<ProposalResponse> deleteSizing(
            @Parameter @PathVariable(name = "sizingId") Long sizingId,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(proposalService.deleteSizing(sizingId, userId));
    }

    // ─── SUBMIT ────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit proposal for approval (DRAFT → SUBMITTED)")
    public ResponseEntity<Map<String, Object>> submit(
            @Parameter @PathVariable(name = "id") Long id,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        proposalService.submit(id, userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Proposal submitted"));
    }

    // ─── APPROVE ───────────────────────────────────────────────────────────────

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve proposal (SUBMITTED → APPROVED)")
    public ResponseEntity<Map<String, Object>> approve(
            @Parameter @PathVariable(name = "id") Long id,
            @RequestParam(name = "level", defaultValue = "1") Integer level,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        proposalService.approveByLevel(id, level, userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Proposal approved"));
    }

    // ─── REJECT ───────────────────────────────────────────────────────────────

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject proposal (SUBMITTED → REJECTED)")
    public ResponseEntity<Map<String, Object>> reject(
            @Parameter @PathVariable(name = "id") Long id,
            @Parameter(description = "User ID (placeholder for auth)") @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        proposalService.reject(id, userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Proposal rejected"));
    }

    // ─── DELETE ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete draft proposal")
    public ResponseEntity<Map<String, Object>> remove(@Parameter @PathVariable(name = "id") Long id) {
        proposalService.remove(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Proposal deleted"));
    }
}
