package com.vieterp.crm.service;

import com.vieterp.crm.domain.Company;
import com.vieterp.crm.exception.CompanyNotFoundException;
import com.vieterp.crm.repository.CompanyRepository;
import com.vieterp.crm.service.dto.CompanyResponse;
import com.vieterp.crm.service.dto.CreateCompanyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock private CompanyRepository companyRepository;

    private CompanyEventPublisherTestDouble eventPublisher = new CompanyEventPublisherTestDouble();

    private CompanyService companyService;

    private Company testCompany;
    private UUID testId;

    @BeforeEach
    void setUp() {
        companyService = new CompanyService(companyRepository, eventPublisher);

        testId = UUID.randomUUID();
        testCompany = Company.builder()
            .id(testId)
            .name("VietERP Corp")
            .domain("vieterp.com")
            .industry("Technology")
            .size(Company.CompanySize.LARGE)
            .email("contact@vieterp.com")
            .phone("02812345678")
            .country("VN")
            .ownerId("owner-123")
            .build();
    }

    @Test
    void create_savesCompanyAndPublishesEvent() {
        CreateCompanyRequest req = new CreateCompanyRequest(
            "VietERP Corp", "vieterp.com", "Technology",
            Company.CompanySize.LARGE, "02812345678", "contact@vieterp.com",
            "https://vieterp.com", "123 Nguyen Hue", "Ho Chi Minh", "HCM",
            "VN", "0123456789", "Notes", "https://logo.url", "owner-123");

        when(companyRepository.save(any(Company.class))).thenReturn(testCompany);
        CompanyResponse resp = companyService.create(req);

        assertNotNull(resp);
        assertEquals(testId, resp.id());
        assertEquals("VietERP Corp", resp.name());
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(companyRepository.findById(testId)).thenReturn(Optional.empty());
        assertThrows(CompanyNotFoundException.class, () -> companyService.getById(testId));
    }

    @Test
    void listAll_returnsAllCompanies() {
        when(companyRepository.findAll()).thenReturn(List.of(testCompany));
        List<CompanyResponse> result = companyService.listAll();
        assertEquals(1, result.size());
        assertEquals(testId, result.get(0).id());
    }

    @Test
    void delete_deletesWhenExists() {
        when(companyRepository.existsById(testId)).thenReturn(true);
        doNothing().when(companyRepository).deleteById(testId);
        companyService.delete(testId);
        verify(companyRepository).deleteById(testId);
    }

    @Test
    void update_updatesCompanyAndPublishesEvent() {
        CreateCompanyRequest req = new CreateCompanyRequest(
            "Updated Corp", "updated.com", "Finance",
            Company.CompanySize.ENTERPRISE, "0999999999", "updated@vieterp.com",
            "https://updated.com", "456 Le Lai", "Hanoi", "HN",
            "VN", "9988776655", "Updated notes", "https://new-logo.url", "owner-456");

        when(companyRepository.findById(testId)).thenReturn(Optional.of(testCompany));
        when(companyRepository.save(any(Company.class))).thenReturn(testCompany);

        CompanyResponse resp = companyService.update(testId, req);

        assertNotNull(resp);
        verify(companyRepository).save(any(Company.class));
        assertTrue(eventPublisher.wasPublishUpdatedCalled());
    }
}
