package com.vieterp.crm.service;

import com.vieterp.crm.domain.Company;
import com.vieterp.crm.event.CompanyEventPublisher;
import com.vieterp.crm.exception.CompanyNotFoundException;
import com.vieterp.crm.repository.CompanyRepository;
import com.vieterp.crm.domain.dto.CompanyResponse;
import com.vieterp.crm.domain.dto.CreateCompanyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyEventPublisher eventPublisher;

    @Transactional
    public CompanyResponse create(CreateCompanyRequest req) {
        Company company = Company.builder()
            .name(req.name())
            .domain(req.domain())
            .industry(req.industry())
            .size(req.size())
            .phone(req.phone())
            .email(req.email())
            .website(req.website())
            .address(req.address())
            .city(req.city())
            .province(req.province())
            .country(req.country() != null ? req.country() : "VN")
            .taxCode(req.taxCode())
            .notes(req.notes())
            .logoUrl(req.logoUrl())
            .ownerId(req.ownerId())
            .build();

        Company saved = companyRepository.save(company);
        eventPublisher.publishCreated(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getById(UUID id) {
        Company company = companyRepository.findById(id)
            .orElseThrow(() -> new CompanyNotFoundException(id));
        return toResponse(company);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> listAll() {
        return companyRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public CompanyResponse update(UUID id, CreateCompanyRequest req) {
        Company company = companyRepository.findById(id)
            .orElseThrow(() -> new CompanyNotFoundException(id));

        company.setName(req.name());
        company.setDomain(req.domain());
        company.setIndustry(req.industry());
        company.setSize(req.size());
        company.setPhone(req.phone());
        company.setEmail(req.email());
        company.setWebsite(req.website());
        company.setAddress(req.address());
        company.setCity(req.city());
        company.setProvince(req.province());
        if (req.country() != null) company.setCountry(req.country());
        company.setTaxCode(req.taxCode());
        company.setNotes(req.notes());
        company.setLogoUrl(req.logoUrl());

        Company saved = companyRepository.save(company);
        eventPublisher.publishUpdated(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!companyRepository.existsById(id)) {
            throw new CompanyNotFoundException(id);
        }
        companyRepository.deleteById(id);
        eventPublisher.publishDeleted(id);
    }

    private CompanyResponse toResponse(Company c) {
        return new CompanyResponse(
            c.getId(), c.getName(), c.getDomain(), c.getIndustry(), c.getSize(),
            c.getPhone(), c.getEmail(), c.getWebsite(), c.getAddress(), c.getCity(),
            c.getProvince(), c.getCountry(), c.getTaxCode(), c.getNotes(), c.getLogoUrl(),
            c.getOwnerId(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
