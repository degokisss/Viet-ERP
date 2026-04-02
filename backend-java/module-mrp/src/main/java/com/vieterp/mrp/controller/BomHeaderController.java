package com.vieterp.mrp.controller;

import com.vieterp.mrp.domain.dto.BomResponse;
import com.vieterp.mrp.domain.dto.CreateBomRequest;
import com.vieterp.mrp.service.BomHeaderService;
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
@RequestMapping("/api/v1/mrp/boms")
@RequiredArgsConstructor
@Tag(name = "BOM Headers", description = "Bill of Materials header management endpoints")
public class BomHeaderController {

    private final BomHeaderService bomHeaderService;

    @PostMapping
    @Operation(summary = "Create a new BOM header")
    public ResponseEntity<BomResponse> create(@Valid @RequestBody CreateBomRequest req) {
        BomResponse resp = bomHeaderService.create(req);
        return ResponseEntity.created(URI.create("/api/v1/mrp/boms/" + resp.id())).body(resp);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get BOM header by ID")
    public ResponseEntity<BomResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(bomHeaderService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List all BOM headers")
    public ResponseEntity<List<BomResponse>> listAll() {
        return ResponseEntity.ok(bomHeaderService.listAll());
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "List BOM headers by tenant")
    public ResponseEntity<List<BomResponse>> listByTenantId(@PathVariable String tenantId) {
        return ResponseEntity.ok(bomHeaderService.listByTenantId(tenantId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update BOM header")
    public ResponseEntity<BomResponse> update(@PathVariable UUID id, @Valid @RequestBody CreateBomRequest req) {
        return ResponseEntity.ok(bomHeaderService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete BOM header")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        bomHeaderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
