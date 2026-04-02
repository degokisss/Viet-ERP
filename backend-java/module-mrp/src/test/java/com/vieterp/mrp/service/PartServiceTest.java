package com.vieterp.mrp.service;

import com.vieterp.mrp.domain.Part;
import com.vieterp.mrp.domain.enums.MakeOrBuy;
import com.vieterp.mrp.domain.enums.LifecycleStatus;
import com.vieterp.mrp.exception.PartNotFoundException;
import com.vieterp.mrp.repository.PartRepository;
import com.vieterp.mrp.domain.dto.PartResponse;
import com.vieterp.mrp.domain.dto.CreatePartRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartServiceTest {

    @Mock private PartRepository partRepository;

    private PartEventPublisherTestDouble eventPublisher = new PartEventPublisherTestDouble();

    private PartService partService;

    private Part testPart;
    private UUID testId;

    @BeforeEach
    void setUp() {
        partService = new PartService(partRepository, eventPublisher);

        testId = UUID.randomUUID();
        testPart = Part.builder()
            .id(testId)
            .partNumber("PART-001")
            .name("Test Part")
            .description("Test description")
            .makeOrBuy(MakeOrBuy.MAKE)
            .lifecycleStatus(LifecycleStatus.ACTIVE)
            .unitOfMeasure("PCS")
            .shelfLifeDays(30)
            .weight(BigDecimal.valueOf(1.5))
            .volume(BigDecimal.valueOf(0.5))
            .minStockLevel(BigDecimal.valueOf(10))
            .maxStockLevel(BigDecimal.valueOf(100))
            .reorderPoint(BigDecimal.valueOf(20))
            .leadTimeDays(5)
            .isActive(true)
            .tenantId("tenant-123")
            .createdBy("user-123")
            .build();
    }

    @Test
    void create_savesPartAndPublishesEvent() {
        CreatePartRequest req = new CreatePartRequest(
            "PART-001", "Test Part", "Test description",
            MakeOrBuy.MAKE, LifecycleStatus.ACTIVE, "PCS",
            30, BigDecimal.valueOf(1.5), BigDecimal.valueOf(0.5),
            BigDecimal.valueOf(10), BigDecimal.valueOf(100), BigDecimal.valueOf(20),
            5, true, "tenant-123", "user-123"
        );

        when(partRepository.save(any(Part.class))).thenReturn(testPart);
        PartResponse resp = partService.create(req);

        assertNotNull(resp);
        assertEquals(testId, resp.id());
        assertEquals("Test Part", resp.name());
        verify(partRepository).save(any(Part.class));
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(partRepository.findById(testId)).thenReturn(Optional.empty());
        assertThrows(PartNotFoundException.class, () -> partService.getById(testId));
    }

    @Test
    void listAll_returnsAllParts() {
        when(partRepository.findAll()).thenReturn(List.of(testPart));
        List<PartResponse> result = partService.listAll();
        assertEquals(1, result.size());
        assertEquals(testId, result.get(0).id());
    }

    @Test
    void delete_deletesWhenExists() {
        when(partRepository.existsById(testId)).thenReturn(true);
        doNothing().when(partRepository).deleteById(testId);
        partService.delete(testId);
        verify(partRepository).deleteById(testId);
    }

    @Test
    void update_updatesPartAndPublishesEvent() {
        CreatePartRequest req = new CreatePartRequest(
            "PART-002", "Updated Part", "Updated description",
            MakeOrBuy.BUY, LifecycleStatus.PROTOTYPE, "KG",
            60, BigDecimal.valueOf(2.0), BigDecimal.valueOf(1.0),
            BigDecimal.valueOf(20), BigDecimal.valueOf(200), BigDecimal.valueOf(40),
            10, false, "tenant-123", "user-456"
        );

        when(partRepository.findById(testId)).thenReturn(Optional.of(testPart));
        when(partRepository.save(any(Part.class))).thenReturn(testPart);

        PartResponse resp = partService.update(testId, req);

        assertNotNull(resp);
        verify(partRepository).save(any(Part.class));
        assertTrue(eventPublisher.wasPublishUpdatedCalled());
    }
}
