package com.vieterp.tpm.controller;

import com.vieterp.tpm.domain.dto.PromotionResponse;
import com.vieterp.tpm.domain.dto.CreatePromotionRequest;
import com.vieterp.tpm.service.PromotionService;
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
@RequestMapping("/api/v1/tpm/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "Promotion management endpoints")
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    @Operation(summary = "Create a new promotion")
    public ResponseEntity<PromotionResponse> create(@Valid @RequestBody CreatePromotionRequest req) {
        PromotionResponse resp = promotionService.create(req);
        return ResponseEntity.created(URI.create("/api/v1/tpm/promotions/" + resp.id())).body(resp);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get promotion by ID")
    public ResponseEntity<PromotionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(promotionService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List all promotions")
    public ResponseEntity<List<PromotionResponse>> listAll() {
        return ResponseEntity.ok(promotionService.listAll());
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "List promotions by tenant")
    public ResponseEntity<List<PromotionResponse>> listByTenantId(@PathVariable String tenantId) {
        return ResponseEntity.ok(promotionService.listByTenantId(tenantId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update promotion")
    public ResponseEntity<PromotionResponse> update(@PathVariable UUID id, @Valid @RequestBody CreatePromotionRequest req) {
        return ResponseEntity.ok(promotionService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete promotion")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        promotionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
