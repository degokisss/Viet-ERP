package com.vieterp.mrp.service;

import com.vieterp.mrp.domain.BomHeader;
import com.vieterp.mrp.domain.enums.BomType;
import com.vieterp.mrp.exception.BomNotFoundException;
import com.vieterp.mrp.repository.BomHeaderRepository;
import com.vieterp.mrp.domain.dto.BomResponse;
import com.vieterp.mrp.domain.dto.CreateBomRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BomHeaderServiceTest {

    @Mock private BomHeaderRepository bomHeaderRepository;

    private BomHeaderEventPublisherTestDouble eventPublisher = new BomHeaderEventPublisherTestDouble();

    private BomHeaderService bomHeaderService;

    private BomHeader testBom;
    private UUID testId;

    @BeforeEach
    void setUp() {
        bomHeaderService = new BomHeaderService(bomHeaderRepository, eventPublisher);

        testId = UUID.randomUUID();
        testBom = BomHeader.builder()
            .id(testId)
            .bomNumber("BOM-001")
            .name("Test BOM")
            .bomType(BomType.MANUFACTURING)
            .partId("part-123")
            .quantity(BigDecimal.valueOf(10))
            .isActive(true)
            .effectiveFrom(LocalDate.of(2025, 1, 1))
            .effectiveTo(LocalDate.of(2025, 12, 31))
            .tenantId("tenant-123")
            .createdBy("user-123")
            .build();
    }

    @Test
    void create_savesBomAndPublishesEvent() {
        CreateBomRequest req = new CreateBomRequest(
            "BOM-001", "Test BOM", BomType.MANUFACTURING,
            "part-123", BigDecimal.valueOf(10),
            true, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
            "tenant-123", "user-123"
        );

        when(bomHeaderRepository.save(any(BomHeader.class))).thenReturn(testBom);
        BomResponse resp = bomHeaderService.create(req);

        assertNotNull(resp);
        assertEquals(testId, resp.id());
        assertEquals("Test BOM", resp.name());
        verify(bomHeaderRepository).save(any(BomHeader.class));
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(bomHeaderRepository.findById(testId)).thenReturn(Optional.empty());
        assertThrows(BomNotFoundException.class, () -> bomHeaderService.getById(testId));
    }

    @Test
    void listAll_returnsAllBoms() {
        when(bomHeaderRepository.findAll()).thenReturn(List.of(testBom));
        List<BomResponse> result = bomHeaderService.listAll();
        assertEquals(1, result.size());
        assertEquals(testId, result.get(0).id());
    }

    @Test
    void delete_deletesWhenExists() {
        when(bomHeaderRepository.existsById(testId)).thenReturn(true);
        doNothing().when(bomHeaderRepository).deleteById(testId);
        bomHeaderService.delete(testId);
        verify(bomHeaderRepository).deleteById(testId);
    }

    @Test
    void update_updatesBomAndPublishesEvent() {
        CreateBomRequest req = new CreateBomRequest(
            "BOM-002", "Updated BOM", BomType.ENGINEERING,
            "part-456", BigDecimal.valueOf(20),
            false, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
            "tenant-123", "user-456"
        );

        when(bomHeaderRepository.findById(testId)).thenReturn(Optional.of(testBom));
        when(bomHeaderRepository.save(any(BomHeader.class))).thenReturn(testBom);

        BomResponse resp = bomHeaderService.update(testId, req);

        assertNotNull(resp);
        verify(bomHeaderRepository).save(any(BomHeader.class));
        assertTrue(eventPublisher.wasPublishUpdatedCalled());
    }
}
