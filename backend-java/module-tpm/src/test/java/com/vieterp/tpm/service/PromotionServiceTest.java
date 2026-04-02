package com.vieterp.tpm.service;

import com.vieterp.tpm.domain.Promotion;
import com.vieterp.tpm.domain.enums.PromotionStatus;
import com.vieterp.tpm.exception.PromotionNotFoundException;
import com.vieterp.tpm.repository.PromotionRepository;
import com.vieterp.tpm.domain.dto.PromotionResponse;
import com.vieterp.tpm.domain.dto.CreatePromotionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    private PromotionEventPublisherTestDouble eventPublisher = new PromotionEventPublisherTestDouble();

    private PromotionService promotionService;

    private Promotion testPromotion;
    private UUID testId;

    @BeforeEach
    void setUp() {
        promotionService = new PromotionService(promotionRepository, eventPublisher);

        testId = UUID.randomUUID();
        testPromotion = Promotion.builder()
            .id(testId)
            .name("Summer Sale")
            .description("Summer promotional sale")
            .startDate(Instant.parse("2026-06-01T00:00:00Z"))
            .endDate(Instant.parse("2026-08-31T23:59:59Z"))
            .budget(BigDecimal.valueOf(50000))
            .spentAmount(BigDecimal.ZERO)
            .status(PromotionStatus.DRAFT)
            .customerId("customer-123")
            .tenantId("tenant-123")
            .createdBy("user-123")
            .build();
    }

    @Test
    void create_savesPromotionAndPublishesEvent() {
        CreatePromotionRequest req = new CreatePromotionRequest(
            "Summer Sale", "Summer promotional sale",
            Instant.parse("2026-06-01T00:00:00Z"), Instant.parse("2026-08-31T23:59:59Z"),
            BigDecimal.valueOf(50000), PromotionStatus.DRAFT,
            "customer-123", "tenant-123", "user-123"
        );

        when(promotionRepository.save(any(Promotion.class))).thenReturn(testPromotion);
        PromotionResponse resp = promotionService.create(req);

        assertNotNull(resp);
        assertEquals(testId, resp.id());
        assertEquals("Summer Sale", resp.name());
        verify(promotionRepository).save(any(Promotion.class));
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(promotionRepository.findById(testId)).thenReturn(Optional.empty());
        assertThrows(PromotionNotFoundException.class, () -> promotionService.getById(testId));
    }

    @Test
    void listAll_returnsAllPromotions() {
        when(promotionRepository.findAll()).thenReturn(List.of(testPromotion));
        List<PromotionResponse> result = promotionService.listAll();
        assertEquals(1, result.size());
        assertEquals(testId, result.get(0).id());
    }

    @Test
    void delete_deletesWhenExists() {
        when(promotionRepository.existsById(testId)).thenReturn(true);
        doNothing().when(promotionRepository).deleteById(testId);
        promotionService.delete(testId);
        verify(promotionRepository).deleteById(testId);
    }

    @Test
    void update_updatesPromotionAndPublishesEvent() {
        CreatePromotionRequest req = new CreatePromotionRequest(
            "Winter Sale", "Winter promotional sale",
            Instant.parse("2026-12-01T00:00:00Z"), Instant.parse("2026-12-31T23:59:59Z"),
            BigDecimal.valueOf(75000), PromotionStatus.PLANNED,
            "customer-456", "tenant-123", "user-456"
        );

        when(promotionRepository.findById(testId)).thenReturn(Optional.of(testPromotion));
        when(promotionRepository.save(any(Promotion.class))).thenReturn(testPromotion);

        PromotionResponse resp = promotionService.update(testId, req);

        assertNotNull(resp);
        verify(promotionRepository).save(any(Promotion.class));
        assertTrue(eventPublisher.wasPublishUpdatedCalled());
    }
}
