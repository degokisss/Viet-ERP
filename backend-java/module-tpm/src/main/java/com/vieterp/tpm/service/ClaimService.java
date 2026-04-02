package com.vieterp.tpm.service;

import com.vieterp.tpm.domain.Claim;
import com.vieterp.tpm.event.ClaimEventPublisher;
import com.vieterp.tpm.exception.ClaimNotFoundException;
import com.vieterp.tpm.repository.ClaimRepository;
import com.vieterp.tpm.domain.dto.ClaimResponse;
import com.vieterp.tpm.domain.dto.CreateClaimRequest;
import com.vieterp.tpm.domain.enums.ClaimStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimEventPublisher eventPublisher;

    @Transactional
    public ClaimResponse create(CreateClaimRequest req) {
        Claim claim = Claim.builder()
            .claimNumber(req.claimNumber())
            .claimType(req.claimType())
            .amount(req.amount())
            .status(req.status() != null ? req.status() : ClaimStatus.DRAFT)
            .customerId(req.customerId())
            .promotionId(req.promotionId())
            .submittedAt(req.submittedAt())
            .approvedAt(req.approvedAt())
            .tenantId(req.tenantId())
            .createdBy(req.createdBy())
            .build();

        Claim saved = claimRepository.save(claim);
        eventPublisher.publishCreated(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ClaimResponse getById(UUID id) {
        Claim claim = claimRepository.findById(id)
            .orElseThrow(() -> new ClaimNotFoundException(id));
        return toResponse(claim);
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> listAll() {
        return claimRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> listByTenantId(String tenantId) {
        return claimRepository.findByTenantId(tenantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public ClaimResponse update(UUID id, CreateClaimRequest req) {
        Claim claim = claimRepository.findById(id)
            .orElseThrow(() -> new ClaimNotFoundException(id));

        claim.setClaimNumber(req.claimNumber());
        claim.setClaimType(req.claimType());
        claim.setAmount(req.amount());
        if (req.status() != null) claim.setStatus(req.status());
        if (req.customerId() != null) claim.setCustomerId(req.customerId());
        if (req.promotionId() != null) claim.setPromotionId(req.promotionId());
        if (req.submittedAt() != null) claim.setSubmittedAt(req.submittedAt());
        if (req.approvedAt() != null) claim.setApprovedAt(req.approvedAt());

        Claim saved = claimRepository.save(claim);
        eventPublisher.publishUpdated(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!claimRepository.existsById(id)) {
            throw new ClaimNotFoundException(id);
        }
        claimRepository.deleteById(id);
        eventPublisher.publishDeleted(id);
    }

    private ClaimResponse toResponse(Claim c) {
        return new ClaimResponse(
            c.getId(), c.getClaimNumber(), c.getClaimType(),
            c.getAmount(), c.getStatus(), c.getCustomerId(),
            c.getPromotionId(), c.getSubmittedAt(), c.getApprovedAt(),
            c.getTenantId(), c.getCreatedBy(), c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
