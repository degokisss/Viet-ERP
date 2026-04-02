package com.vieterp.crm.controller;

import com.vieterp.crm.service.dto.ContactResponse;
import com.vieterp.crm.service.dto.CreateContactRequest;
import com.vieterp.crm.service.ContactService;
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
@RequestMapping("/api/v1/crm/contacts")
@RequiredArgsConstructor
@Tag(name = "Contacts", description = "Contact management endpoints")
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    @Operation(summary = "Create a new contact")
    public ResponseEntity<ContactResponse> create(@Valid @RequestBody CreateContactRequest req) {
        ContactResponse resp = contactService.create(req);
        return ResponseEntity.created(URI.create("/api/v1/crm/contacts/" + resp.id())).body(resp);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get contact by ID")
    public ResponseEntity<ContactResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(contactService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List all contacts")
    public ResponseEntity<List<ContactResponse>> listAll() {
        return ResponseEntity.ok(contactService.listAll());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update contact")
    public ResponseEntity<ContactResponse> update(@PathVariable UUID id, @Valid @RequestBody CreateContactRequest req) {
        return ResponseEntity.ok(contactService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete contact")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        contactService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
