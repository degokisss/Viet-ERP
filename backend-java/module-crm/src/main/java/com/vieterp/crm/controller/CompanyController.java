package com.vieterp.crm.controller;

import com.vieterp.crm.domain.dto.CompanyResponse;
import com.vieterp.crm.domain.dto.CreateCompanyRequest;
import com.vieterp.crm.service.CompanyService;
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
@RequestMapping("/api/v1/crm/companies")
@RequiredArgsConstructor
@Tag(name = "Companies", description = "Company management endpoints")
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @Operation(summary = "Create a new company")
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CreateCompanyRequest req) {
        CompanyResponse resp = companyService.create(req);
        return ResponseEntity.created(URI.create("/api/v1/crm/companies/" + resp.id())).body(resp);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get company by ID")
    public ResponseEntity<CompanyResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(companyService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List all companies")
    public ResponseEntity<List<CompanyResponse>> listAll() {
        return ResponseEntity.ok(companyService.listAll());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update company")
    public ResponseEntity<CompanyResponse> update(@PathVariable UUID id, @Valid @RequestBody CreateCompanyRequest req) {
        return ResponseEntity.ok(companyService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete company")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
