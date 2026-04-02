package com.vieterp.mrp.service;

import com.vieterp.mrp.domain.Part;
import com.vieterp.mrp.event.PartEventPublisher;
import com.vieterp.mrp.exception.PartNotFoundException;
import com.vieterp.mrp.repository.PartRepository;
import com.vieterp.mrp.domain.dto.PartResponse;
import com.vieterp.mrp.domain.dto.CreatePartRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartService {

    private final PartRepository partRepository;
    private final PartEventPublisher eventPublisher;

    @Transactional
    public PartResponse create(CreatePartRequest req) {
        Part part = Part.builder()
            .partNumber(req.partNumber())
            .name(req.name())
            .description(req.description())
            .makeOrBuy(req.makeOrBuy())
            .lifecycleStatus(req.lifecycleStatus())
            .unitOfMeasure(req.unitOfMeasure())
            .shelfLifeDays(req.shelfLifeDays())
            .weight(req.weight())
            .volume(req.volume())
            .minStockLevel(req.minStockLevel())
            .maxStockLevel(req.maxStockLevel())
            .reorderPoint(req.reorderPoint())
            .leadTimeDays(req.leadTimeDays())
            .isActive(req.isActive() != null ? req.isActive() : true)
            .tenantId(req.tenantId())
            .createdBy(req.createdBy())
            .build();

        Part saved = partRepository.save(part);
        eventPublisher.publishCreated(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PartResponse getById(UUID id) {
        Part part = partRepository.findById(id)
            .orElseThrow(() -> new PartNotFoundException(id));
        return toResponse(part);
    }

    @Transactional(readOnly = true)
    public List<PartResponse> listAll() {
        return partRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PartResponse> listByTenantId(String tenantId) {
        return partRepository.findByTenantId(tenantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public PartResponse update(UUID id, CreatePartRequest req) {
        Part part = partRepository.findById(id)
            .orElseThrow(() -> new PartNotFoundException(id));

        part.setPartNumber(req.partNumber());
        part.setName(req.name());
        part.setDescription(req.description());
        part.setMakeOrBuy(req.makeOrBuy());
        part.setLifecycleStatus(req.lifecycleStatus());
        part.setUnitOfMeasure(req.unitOfMeasure());
        if (req.shelfLifeDays() != null) part.setShelfLifeDays(req.shelfLifeDays());
        if (req.weight() != null) part.setWeight(req.weight());
        if (req.volume() != null) part.setVolume(req.volume());
        if (req.minStockLevel() != null) part.setMinStockLevel(req.minStockLevel());
        if (req.maxStockLevel() != null) part.setMaxStockLevel(req.maxStockLevel());
        if (req.reorderPoint() != null) part.setReorderPoint(req.reorderPoint());
        if (req.leadTimeDays() != null) part.setLeadTimeDays(req.leadTimeDays());
        if (req.isActive() != null) part.setIsActive(req.isActive());

        Part saved = partRepository.save(part);
        eventPublisher.publishUpdated(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!partRepository.existsById(id)) {
            throw new PartNotFoundException(id);
        }
        partRepository.deleteById(id);
        eventPublisher.publishDeleted(id);
    }

    private PartResponse toResponse(Part p) {
        return new PartResponse(
            p.getId(), p.getPartNumber(), p.getName(), p.getDescription(),
            p.getMakeOrBuy(), p.getLifecycleStatus(), p.getUnitOfMeasure(),
            p.getShelfLifeDays(), p.getWeight(), p.getVolume(),
            p.getMinStockLevel(), p.getMaxStockLevel(), p.getReorderPoint(),
            p.getLeadTimeDays(), p.getIsActive(), p.getTenantId(),
            p.getCreatedBy(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
