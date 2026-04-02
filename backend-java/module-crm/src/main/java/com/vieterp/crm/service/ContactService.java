package com.vieterp.crm.service;

import com.vieterp.crm.domain.Company;
import com.vieterp.crm.domain.Contact;
import com.vieterp.crm.event.ContactEventPublisher;
import com.vieterp.crm.exception.ContactNotFoundException;
import com.vieterp.crm.repository.CompanyRepository;
import com.vieterp.crm.repository.ContactRepository;
import com.vieterp.crm.domain.dto.ContactResponse;
import com.vieterp.crm.domain.dto.CreateContactRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;
    private final ContactEventPublisher eventPublisher;

    @Transactional
    public ContactResponse create(CreateContactRequest req) {
        Contact contact = Contact.builder()
            .firstName(req.firstName())
            .lastName(req.lastName())
            .email(req.email())
            .phone(req.phone())
            .mobile(req.mobile())
            .jobTitle(req.jobTitle())
            .department(req.department())
            .avatarUrl(req.avatarUrl())
            .notes(req.notes())
            .source(req.source())
            .status(req.status() != null ? req.status() : Contact.ContactStatus.ACTIVE)
            .country(req.country())
            .externalHrmId(req.externalHrmId())
            .score(req.score() != null ? req.score() : 0)
            .ownerId(req.ownerId())
            .build();

        if (req.companyId() != null) {
            Company company = companyRepository.findById(req.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + req.companyId()));
            contact.setCompany(company);
        }

        Contact saved = contactRepository.save(contact);
        eventPublisher.publishCreated(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ContactResponse getById(UUID id) {
        Contact contact = contactRepository.findById(id)
            .orElseThrow(() -> new ContactNotFoundException(id));
        return toResponse(contact);
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> listAll() {
        return contactRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public ContactResponse update(UUID id, CreateContactRequest req) {
        Contact contact = contactRepository.findById(id)
            .orElseThrow(() -> new ContactNotFoundException(id));

        contact.setFirstName(req.firstName());
        contact.setLastName(req.lastName());
        contact.setEmail(req.email());
        contact.setPhone(req.phone());
        contact.setMobile(req.mobile());
        contact.setJobTitle(req.jobTitle());
        contact.setDepartment(req.department());
        contact.setAvatarUrl(req.avatarUrl());
        contact.setNotes(req.notes());
        if (req.source() != null) contact.setSource(req.source());
        if (req.status() != null) contact.setStatus(req.status());
        if (req.country() != null) contact.setCountry(req.country());
        contact.setExternalHrmId(req.externalHrmId());
        if (req.score() != null) contact.setScore(req.score());

        if (req.companyId() != null) {
            Company company = companyRepository.findById(req.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + req.companyId()));
            contact.setCompany(company);
        } else {
            contact.setCompany(null);
        }

        Contact saved = contactRepository.save(contact);
        eventPublisher.publishUpdated(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!contactRepository.existsById(id)) {
            throw new ContactNotFoundException(id);
        }
        contactRepository.deleteById(id);
        eventPublisher.publishDeleted(id);
    }

    private ContactResponse toResponse(Contact c) {
        return new ContactResponse(
            c.getId(), c.getFirstName(), c.getLastName(), c.getEmail(),
            c.getPhone(), c.getMobile(), c.getJobTitle(), c.getDepartment(),
            c.getAvatarUrl(), c.getNotes(), c.getSource(), c.getStatus(),
            c.getCompany() != null ? c.getCompany().getId() : null,
            c.getCompany() != null ? c.getCompany().getName() : null,
            c.getCountry(), c.getExternalHrmId(), c.getScore(),
            c.getLastActivityAt(), c.getOwnerId(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
