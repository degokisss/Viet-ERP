package com.vieterp.mrp.controller;

import com.vieterp.mrp.domain.dto.PartResponse;
import com.vieterp.mrp.domain.dto.CreatePartRequest;
import com.vieterp.mrp.service.PartService;
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
@RequestMapping("/api/v1/mrp/parts")
@RequiredArgsConstructor
@Tag(name = "Parts", description = "Part management endpoints")
public class PartController {

    private final PartService partService;

    @PostMapping
    @Operation(summary = "Create a new part")
    public ResponseEntity<PartResponse> create(@Valid @RequestBody CreatePartRequest req) {
        PartResponse resp = partService.create(req);
        return ResponseEntity.created(URI.create("/api/v1/mrp/parts/" + resp.id())).body(resp);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get part by ID")
    public ResponseEntity<PartResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(partService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List all parts")
    public ResponseEntity<List<PartResponse>> listAll() {
        return ResponseEntity.ok(partService.listAll());
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "List parts by tenant")
    public ResponseEntity<List<PartResponse>> listByTenantId(@PathVariable String tenantId) {
        return ResponseEntity.ok(partService.listByTenantId(tenantId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update part")
    public ResponseEntity<PartResponse> update(@PathVariable UUID id, @Valid @RequestBody CreatePartRequest req) {
        return ResponseEntity.ok(partService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete part")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        partService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
