package com.vieterp.accounting.controller;

import com.vieterp.accounting.domain.dto.AccountResponse;
import com.vieterp.accounting.domain.dto.CreateAccountRequest;
import com.vieterp.accounting.service.AccountService;
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
@RequestMapping("/api/v1/accounting/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Chart of Accounts management endpoints")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Create a new account")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest req) {
        AccountResponse resp = accountService.create(req);
        return ResponseEntity.created(URI.create("/api/v1/accounting/accounts/" + resp.id())).body(resp);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<AccountResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List all accounts")
    public ResponseEntity<List<AccountResponse>> listAll() {
        return ResponseEntity.ok(accountService.listAll());
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "List accounts by tenant")
    public ResponseEntity<List<AccountResponse>> listByTenantId(@PathVariable String tenantId) {
        return ResponseEntity.ok(accountService.listByTenantId(tenantId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update account")
    public ResponseEntity<AccountResponse> update(@PathVariable UUID id, @Valid @RequestBody CreateAccountRequest req) {
        return ResponseEntity.ok(accountService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete account")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
