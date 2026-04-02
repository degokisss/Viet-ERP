package com.vieterp.mrp.service;

import com.vieterp.mrp.domain.BomHeader;
import com.vieterp.mrp.event.BomHeaderEventPublisher;
import com.vieterp.mrp.exception.BomNotFoundException;
import com.vieterp.mrp.repository.BomHeaderRepository;
import com.vieterp.mrp.domain.dto.BomResponse;
import com.vieterp.mrp.domain.dto.CreateBomRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BomHeaderService {

    private final BomHeaderRepository bomHeaderRepository;
    private final BomHeaderEventPublisher eventPublisher;

    @Transactional
    public BomResponse create(CreateBomRequest req) {
        BomHeader bom = BomHeader.builder()
            .bomNumber(req.bomNumber())
            .name(req.name())
            .bomType(req.bomType())
            .partId(req.partId())
            .quantity(req.quantity())
            .isActive(req.isActive() != null ? req.isActive() : true)
            .effectiveFrom(req.effectiveFrom())
            .effectiveTo(req.effectiveTo())
            .tenantId(req.tenantId())
            .createdBy(req.createdBy())
            .build();

        BomHeader saved = bomHeaderRepository.save(bom);
        eventPublisher.publishCreated(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BomResponse getById(UUID id) {
        BomHeader bom = bomHeaderRepository.findById(id)
            .orElseThrow(() -> new BomNotFoundException(id));
        return toResponse(bom);
    }

    @Transactional(readOnly = true)
    public List<BomResponse> listAll() {
        return bomHeaderRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BomResponse> listByTenantId(String tenantId) {
        return bomHeaderRepository.findByTenantId(tenantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public BomResponse update(UUID id, CreateBomRequest req) {
        BomHeader bom = bomHeaderRepository.findById(id)
            .orElseThrow(() -> new BomNotFoundException(id));

        bom.setBomNumber(req.bomNumber());
        bom.setName(req.name());
        bom.setBomType(req.bomType());
        bom.setPartId(req.partId());
        if (req.quantity() != null) bom.setQuantity(req.quantity());
        if (req.isActive() != null) bom.setIsActive(req.isActive());
        if (req.effectiveFrom() != null) bom.setEffectiveFrom(req.effectiveFrom());
        if (req.effectiveTo() != null) bom.setEffectiveTo(req.effectiveTo());

        BomHeader saved = bomHeaderRepository.save(bom);
        eventPublisher.publishUpdated(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!bomHeaderRepository.existsById(id)) {
            throw new BomNotFoundException(id);
        }
        bomHeaderRepository.deleteById(id);
        eventPublisher.publishDeleted(id);
    }

    private BomResponse toResponse(BomHeader b) {
        return new BomResponse(
            b.getId(), b.getBomNumber(), b.getName(), b.getBomType(),
            b.getPartId(), b.getQuantity(), b.getIsActive(),
            b.getEffectiveFrom(), b.getEffectiveTo(), b.getTenantId(),
            b.getCreatedBy(), b.getCreatedAt(), b.getUpdatedAt()
        );
    }
}
