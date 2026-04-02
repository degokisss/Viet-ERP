package com.vieterp.tpm.controller;

import com.vieterp.tpm.domain.dto.ClaimResponse;
import com.vieterp.tpm.domain.dto.CreateClaimRequest;
import com.vieterp.tpm.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tpm/claims")
@RequiredArgsConstructor
@Tag(name = "Claims", description = "Claim management endpoints")
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    @Operation(summary = "Create a new claim")
    public ResponseEntity<ClaimResponse> create(@Valid @RequestBody CreateClaimRequest req) {
        ClaimResponse resp = claimService.create(req);
        return ResponseEntity.created(URI.create("/api/v1/tpm/claims/" + resp.id())).body(resp);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get claim by ID")
    public ResponseEntity<ClaimResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(claimService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List all claims")
    public ResponseEntity<List<ClaimResponse>> listAll() {
        return ResponseEntity.ok(claimService.listAll());
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "List claims by tenant")
    public ResponseEntity<List<ClaimResponse>> listByTenantId(@PathVariable String tenantId) {
        return ResponseEntity.ok(claimService.listByTenantId(tenantId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update claim")
    public ResponseEntity<ClaimResponse> update(@PathVariable UUID id, @Valid @RequestBody CreateClaimRequest req) {
        return ResponseEntity.ok(claimService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete claim")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        claimService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
