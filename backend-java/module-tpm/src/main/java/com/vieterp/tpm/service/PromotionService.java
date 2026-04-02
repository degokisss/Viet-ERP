package com.vieterp.tpm.service;

import com.vieterp.tpm.domain.Promotion;
import com.vieterp.tpm.event.PromotionEventPublisher;
import com.vieterp.tpm.exception.PromotionNotFoundException;
import com.vieterp.tpm.repository.PromotionRepository;
import com.vieterp.tpm.domain.dto.PromotionResponse;
import com.vieterp.tpm.domain.dto.CreatePromotionRequest;
import com.vieterp.tpm.domain.enums.PromotionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionEventPublisher eventPublisher;

    @Transactional
    public PromotionResponse create(CreatePromotionRequest req) {
        Promotion promotion = Promotion.builder()
            .name(req.name())
            .description(req.description())
            .startDate(req.startDate())
            .endDate(req.endDate())
            .budget(req.budget())
            .spentAmount(BigDecimal.ZERO)
            .status(req.status() != null ? req.status() : PromotionStatus.DRAFT)
            .customerId(req.customerId())
            .tenantId(req.tenantId())
            .createdBy(req.createdBy())
            .build();

        Promotion saved = promotionRepository.save(promotion);
        eventPublisher.publishCreated(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PromotionResponse getById(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
            .orElseThrow(() -> new PromotionNotFoundException(id));
        return toResponse(promotion);
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> listAll() {
        return promotionRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> listByTenantId(String tenantId) {
        return promotionRepository.findByTenantId(tenantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public PromotionResponse update(UUID id, CreatePromotionRequest req) {
        Promotion promotion = promotionRepository.findById(id)
            .orElseThrow(() -> new PromotionNotFoundException(id));

        promotion.setName(req.name());
        promotion.setDescription(req.description());
        promotion.setStartDate(req.startDate());
        promotion.setEndDate(req.endDate());
        promotion.setBudget(req.budget());
        if (req.status() != null) promotion.setStatus(req.status());
        if (req.customerId() != null) promotion.setCustomerId(req.customerId());

        Promotion saved = promotionRepository.save(promotion);
        eventPublisher.publishUpdated(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!promotionRepository.existsById(id)) {
            throw new PromotionNotFoundException(id);
        }
        promotionRepository.deleteById(id);
        eventPublisher.publishDeleted(id);
    }

    private PromotionResponse toResponse(Promotion p) {
        return new PromotionResponse(
            p.getId(), p.getName(), p.getDescription(),
            p.getStartDate(), p.getEndDate(), p.getBudget(),
            p.getSpentAmount(), p.getStatus(), p.getCustomerId(),
            p.getTenantId(), p.getCreatedBy(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
