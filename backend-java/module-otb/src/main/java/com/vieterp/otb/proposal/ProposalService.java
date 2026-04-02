package com.vieterp.otb.proposal;

import com.vieterp.otb.domain.*;
import com.vieterp.otb.domain.repository.*;
import com.vieterp.otb.proposal.dto.*;
import com.vieterp.otb.proposal.exception.ProposalNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProposalService {

    private final SKUProposalHeaderRepository proposalHeaderRepository;
    private final SKUProposalRepository proposalRepository;
    private final SKUAllocateRepository allocateRepository;
    private final ProposalSizingHeaderRepository sizingHeaderRepository;
    private final ProposalSizingRepository sizingRepository;
    private final AllocateHeaderRepository allocateHeaderRepository;
    private final UserRepository userRepository;

    // ─── LIST ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> findAll(String status, int page, int pageSize) {
        Specification<SKUProposalHeader> spec = Specification.where(null);
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), status));
        }

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SKUProposalHeader> pageResult = proposalHeaderRepository.findAll(spec, pageable);

        List<ProposalResponse> data = pageResult.getContent().stream()
            .map(this::toResponse)
            .toList();

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", page);
        meta.put("pageSize", pageSize);
        meta.put("total", pageResult.getTotalElements());
        meta.put("totalPages", pageResult.getTotalPages());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("meta", meta);
        return result;
    }

    // ─── GET ONE ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProposalResponse findById(Long id) {
        SKUProposalHeader header = proposalHeaderRepository.findById(id)
            .orElseThrow(() -> new ProposalNotFoundException(id.toString()));
        return toResponse(header);
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    public ProposalResponse create(CreateProposalRequest dto, Long userId) {
        User creator = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        AllocateHeader allocateHeader = null;
        if (dto.allocateHeaderId() != null && !dto.allocateHeaderId().isBlank()) {
            allocateHeader = allocateHeaderRepository.findById(Long.parseLong(dto.allocateHeaderId()))
                .orElseThrow(() -> new IllegalArgumentException("AllocateHeader not found: " + dto.allocateHeaderId()));
        }

        final AllocateHeader finalAllocateHeader = allocateHeader;
        Integer latestVersion = proposalHeaderRepository.findAll().stream()
            .filter(h -> finalAllocateHeader != null
                ? Objects.equals(h.getAllocateHeader() != null ? h.getAllocateHeader().getId() : null, finalAllocateHeader.getId())
                : h.getAllocateHeader() == null)
            .map(SKUProposalHeader::getVersion)
            .filter(Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(0);

        SKUProposalHeader header = SKUProposalHeader.builder()
            .allocateHeader(allocateHeader)
            .version(latestVersion + 1)
            .status("DRAFT")
            .isFinalVersion(dto.isFinalVersion() != null ? dto.isFinalVersion() : false)
            .creator(creator)
            .createdAt(Instant.now())
            .build();

        header = proposalHeaderRepository.save(header);
        return toResponse(header);
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    public ProposalResponse update(Long id, UpdateProposalRequest dto, Long userId) {
        SKUProposalHeader header = proposalHeaderRepository.findById(id)
            .orElseThrow(() -> new ProposalNotFoundException(id.toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }

        if (dto.isFinalVersion() != null) {
            header.setIsFinalVersion(dto.isFinalVersion());
        }

        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        header = proposalHeaderRepository.save(header);

        return toResponse(header);
    }

    // ─── SAVE FULL PROPOSAL ────────────────────────────────────────────────────

    public ProposalResponse saveFullProposal(Long headerId, SaveFullProposalRequest dto, Long userId) {
        SKUProposalHeader header = proposalHeaderRepository.findById(headerId)
            .orElseThrow(() -> new ProposalNotFoundException(headerId.toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }

        User updater = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Replace all proposals, allocations, sizing headers, and sizings
        List<SKUProposal> existingProposals = proposalRepository.findBySkuProposalHeaderId(headerId);
        for (SKUProposal p : existingProposals) {
            allocateRepository.deleteBySkuProposalId(p.getId());
        }
        proposalRepository.deleteBySkuProposalHeaderId(headerId);

        List<ProposalSizingHeader> existingSizingHeaders = sizingHeaderRepository.findBySkuProposalHeaderId(headerId);
        for (ProposalSizingHeader sh : existingSizingHeaders) {
            sizingRepository.deleteByProposalSizingHeaderId(sh.getId());
        }
        sizingHeaderRepository.deleteBySkuProposalHeaderId(headerId);

        // Bulk add products
        if (dto.products() != null) {
            for (CreateSKUProposalRequest prodReq : dto.products()) {
                SKUProposal proposal = SKUProposal.builder()
                    .skuProposalHeaderId(headerId)
                    .productId(Long.parseLong(prodReq.productId()))
                    .customerTarget(prodReq.customerTarget())
                    .unitCost(prodReq.unitCost())
                    .srp(prodReq.srp())
                    .createdBy(userId)
                    .createdAt(Instant.now())
                    .build();
                proposal = proposalRepository.save(proposal);
            }
        }

        // Bulk add allocations
        if (dto.allocations() != null) {
            for (FullAllocationDto allocDto : dto.allocations()) {
                SKUAllocate alloc = SKUAllocate.builder()
                    .skuProposalId(Long.parseLong(allocDto.getSkuProposalId()))
                    .storeId(Long.parseLong(allocDto.getStoreId()))
                    .quantity(allocDto.getQuantity())
                    .createdBy(userId)
                    .createdAt(Instant.now())
                    .build();
                allocateRepository.save(alloc);
            }
        }

        // Bulk add sizing headers and sizings
        if (dto.sizingHeaders() != null) {
            for (FullSizingHeaderDto shDto : dto.sizingHeaders()) {
                int version = sizingHeaderRepository.findBySkuProposalHeaderId(headerId).size() + 1;
                ProposalSizingHeader sh = ProposalSizingHeader.builder()
                    .skuProposalHeaderId(headerId)
                    .version(version)
                    .isFinalVersion(shDto.getIsFinalVersion() != null ? shDto.getIsFinalVersion() : false)
                    .createdBy(userId)
                    .createdAt(Instant.now())
                    .build();
                sh = sizingHeaderRepository.save(sh);

                if (shDto.getSizings() != null) {
                    for (FullSizingDto sDto : shDto.getSizings()) {
                        ProposalSizing sizing = ProposalSizing.builder()
                            .proposalSizingHeaderId(sh.getId())
                            .skuProposalId(Long.parseLong(sDto.getSkuProposalId()))
                            .subcategorySizeId(Long.parseLong(sDto.getSubcategorySizeId()))
                            .actualSalesmixPct(sDto.getActualSalesmixPct())
                            .actualStPct(sDto.getActualStPct())
                            .proposalQuantity(sDto.getProposalQuantity())
                            .createdBy(userId)
                            .createdAt(Instant.now())
                            .build();
                        sizingRepository.save(sizing);
                    }
                }
            }
        }

        header.setUpdater(updater);
        header.setUpdatedAt(Instant.now());
        proposalHeaderRepository.save(header);

        return toResponse(header);
    }

    // ─── COPY PROPOSAL ────────────────────────────────────────────────────────

    public ProposalResponse copyProposal(Long id, Long userId) {
        SKUProposalHeader original = proposalHeaderRepository.findById(id)
            .orElseThrow(() -> new ProposalNotFoundException(id.toString()));
        User creator = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        SKUProposalHeader newHeader = SKUProposalHeader.builder()
            .allocateHeader(original.getAllocateHeader())
            .version(original.getVersion() + 1)
            .status("DRAFT")
            .isFinalVersion(false)
            .creator(creator)
            .createdAt(Instant.now())
            .build();
        newHeader = proposalHeaderRepository.save(newHeader);

        // Copy proposals
        List<SKUProposal> originalProposals = proposalRepository.findBySkuProposalHeaderId(id);
        Map<Long, Long> oldToNewProposalId = new HashMap<>();
        for (SKUProposal op : originalProposals) {
            SKUProposal np = SKUProposal.builder()
                .skuProposalHeaderId(newHeader.getId())
                .productId(op.getProductId())
                .customerTarget(op.getCustomerTarget())
                .unitCost(op.getUnitCost())
                .srp(op.getSrp())
                .createdBy(userId)
                .createdAt(Instant.now())
                .build();
            np = proposalRepository.save(np);
            oldToNewProposalId.put(op.getId(), np.getId());

            // Copy allocations
            List<SKUAllocate> originalAllocs = allocateRepository.findBySkuProposalId(op.getId());
            for (SKUAllocate oa : originalAllocs) {
                SKUAllocate na = SKUAllocate.builder()
                    .skuProposalId(np.getId())
                    .storeId(oa.getStoreId())
                    .quantity(oa.getQuantity())
                    .createdBy(userId)
                    .createdAt(Instant.now())
                    .build();
                allocateRepository.save(na);
            }
        }

        // Copy sizing headers
        List<ProposalSizingHeader> originalSizingHeaders = sizingHeaderRepository.findBySkuProposalHeaderId(id);
        for (ProposalSizingHeader osh : originalSizingHeaders) {
            ProposalSizingHeader nsh = ProposalSizingHeader.builder()
                .skuProposalHeaderId(newHeader.getId())
                .version(osh.getVersion())
                .isFinalVersion(false)
                .createdBy(userId)
                .createdAt(Instant.now())
                .build();
            nsh = sizingHeaderRepository.save(nsh);

            List<ProposalSizing> originalSizings = sizingRepository.findByProposalSizingHeaderId(osh.getId());
            for (ProposalSizing os : originalSizings) {
                Long newProposalId = oldToNewProposalId.get(os.getSkuProposalId());
                if (newProposalId != null) {
                    ProposalSizing ns = ProposalSizing.builder()
                        .proposalSizingHeaderId(nsh.getId())
                        .skuProposalId(newProposalId)
                        .subcategorySizeId(os.getSubcategorySizeId())
                        .actualSalesmixPct(os.getActualSalesmixPct())
                        .actualStPct(os.getActualStPct())
                        .proposalQuantity(os.getProposalQuantity())
                        .createdBy(userId)
                        .createdAt(Instant.now())
                        .build();
                    sizingRepository.save(ns);
                }
            }
        }

        return toResponse(newHeader);
    }

    // ─── ADD PRODUCT ──────────────────────────────────────────────────────────

    public ProposalResponse addProduct(Long proposalId, CreateSKUProposalRequest dto, Long userId) {
        SKUProposalHeader header = proposalHeaderRepository.findById(proposalId)
            .orElseThrow(() -> new ProposalNotFoundException(proposalId.toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }

        SKUProposal proposal = SKUProposal.builder()
            .skuProposalHeaderId(proposalId)
            .productId(Long.parseLong(dto.productId()))
            .customerTarget(dto.customerTarget())
            .unitCost(dto.unitCost())
            .srp(dto.srp())
            .createdBy(userId)
            .createdAt(Instant.now())
            .build();
        proposalRepository.save(proposal);

        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        proposalHeaderRepository.save(header);

        return toResponse(header);
    }

    // ─── BULK ADD PRODUCTS ───────────────────────────────────────────────────

    public ProposalResponse bulkAddProducts(Long proposalId, BulkAddProductsRequest dto, Long userId) {
        SKUProposalHeader header = proposalHeaderRepository.findById(proposalId)
            .orElseThrow(() -> new ProposalNotFoundException(proposalId.toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }

        for (CreateSKUProposalRequest prodReq : dto.products()) {
            SKUProposal proposal = SKUProposal.builder()
                .skuProposalHeaderId(proposalId)
                .productId(Long.parseLong(prodReq.productId()))
                .customerTarget(prodReq.customerTarget())
                .unitCost(prodReq.unitCost())
                .srp(prodReq.srp())
                .createdBy(userId)
                .createdAt(Instant.now())
                .build();
            proposalRepository.save(proposal);
        }

        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        proposalHeaderRepository.save(header);

        return toResponse(header);
    }

    // ─── UPDATE PROPOSAL ITEM ─────────────────────────────────────────────────

    public ProposalResponse updateProposal(Long itemId, UpdateSKUProposalRequest dto, Long userId) {
        SKUProposal proposal = proposalRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("SKUProposal not found: " + itemId));

        SKUProposalHeader header = proposalHeaderRepository.findById(proposal.getSkuProposalHeaderId())
            .orElseThrow(() -> new ProposalNotFoundException(proposal.getSkuProposalHeaderId().toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }

        if (dto.customerTarget() != null) proposal.setCustomerTarget(dto.customerTarget());
        if (dto.unitCost() != null) proposal.setUnitCost(dto.unitCost());
        if (dto.srp() != null) proposal.setSrp(dto.srp());

        proposal.setUpdatedBy(userId);
        proposal.setUpdatedAt(Instant.now());
        proposalRepository.save(proposal);

        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        proposalHeaderRepository.save(header);

        return toResponse(header);
    }

    // ─── REMOVE PROPOSAL ITEM ────────────────────────────────────────────────

    public ProposalResponse removeProposal(Long itemId, Long userId) {
        SKUProposal proposal = proposalRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("SKUProposal not found: " + itemId));

        Long headerId = proposal.getSkuProposalHeaderId();
        SKUProposalHeader header = proposalHeaderRepository.findById(headerId)
            .orElseThrow(() -> new ProposalNotFoundException(headerId.toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }

        allocateRepository.deleteBySkuProposalId(itemId);
        proposalRepository.delete(proposal);

        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        proposalHeaderRepository.save(header);

        return toResponse(header);
    }

    // ─── CREATE ALLOCATIONS ──────────────────────────────────────────────────

    public ProposalResponse createAllocations(Long proposalId, CreateAllocateRequest dto, Long userId) {
        SKUProposalHeader header = proposalHeaderRepository.findById(proposalId)
            .orElseThrow(() -> new ProposalNotFoundException(proposalId.toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }

        for (SKUAllocateDto allocDto : dto.allocations()) {
            SKUAllocate alloc = SKUAllocate.builder()
                .skuProposalId(Long.parseLong(allocDto.getSkuProposalId()))
                .storeId(Long.parseLong(allocDto.getStoreId()))
                .quantity(allocDto.getQuantity())
                .createdBy(userId)
                .createdAt(Instant.now())
                .build();
            allocateRepository.save(alloc);
        }

        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        proposalHeaderRepository.save(header);

        return toResponse(header);
    }

    // ─── GET STORE ALLOCATIONS ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProposalResponse.AllocateSummary> getStoreAllocations(Long proposalId) {
        List<SKUAllocate> allocs = allocateRepository.findBySkuProposalId(proposalId);
        return allocs.stream()
            .map(a -> ProposalResponse.AllocateSummary.builder()
                .id(a.getId())
                .storeId(a.getStoreId())
                .quantity(a.getQuantity())
                .build())
            .toList();
    }

    // ─── UPDATE ALLOCATION ───────────────────────────────────────────────────

    public ProposalResponse updateAllocation(Long allocationId, UpdateAllocateRequest dto, Long userId) {
        SKUAllocate alloc = allocateRepository.findById(allocationId)
            .orElseThrow(() -> new IllegalArgumentException("SKUAllocate not found: " + allocationId));

        SKUProposalHeader header = proposalHeaderRepository.findById(alloc.getSkuProposalId())
            .orElseThrow(() -> new ProposalNotFoundException(alloc.getSkuProposalId().toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }

        if (dto.quantity() != null) alloc.setQuantity(dto.quantity());
        alloc.setUpdatedBy(userId);
        alloc.setUpdatedAt(Instant.now());
        allocateRepository.save(alloc);

        // Reload to get the header's proposals to find the actual proposal header id
        List<SKUProposal> proposals = proposalRepository.findBySkuProposalHeaderId(header.getId());
        Long skuProposalHeaderId = header.getId();
        // Actually the alloc has skuProposalId which is the SKUProposal item, we need to find the header
        // Let's recompute: alloc.skuProposalId is SKUProposal.id, we find that proposal's header id
        for (SKUProposal p : proposals) {
            if (p.getId().equals(alloc.getSkuProposalId())) {
                skuProposalHeaderId = p.getSkuProposalHeaderId();
                break;
            }
        }
        final Long finalSkuProposalHeaderId = skuProposalHeaderId;
        SKUProposalHeader h = proposalHeaderRepository.findById(finalSkuProposalHeaderId)
            .orElseThrow(() -> new ProposalNotFoundException(finalSkuProposalHeaderId.toString()));
        h.setUpdater(userRepository.getReferenceById(userId));
        h.setUpdatedAt(Instant.now());
        proposalHeaderRepository.save(h);

        return toResponse(h);
    }

    // ─── DELETE ALLOCATION ───────────────────────────────────────────────────

    public void deleteAllocation(Long allocationId) {
        allocateRepository.deleteById(allocationId);
    }

    // ─── CREATE SIZING HEADER ───────────────────────────────────────────────

    public ProposalResponse createSizingHeader(Long proposalId, CreateSizingHeaderRequest dto, Long userId) {
        SKUProposalHeader header = proposalHeaderRepository.findById(proposalId)
            .orElseThrow(() -> new ProposalNotFoundException(proposalId.toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }

        long count = sizingHeaderRepository.countBySkuProposalHeaderId(proposalId);
        if (count >= 3) {
            throw new IllegalStateException("Maximum of 3 sizing headers allowed per proposal");
        }

        int version = (int) count + 1;

        ProposalSizingHeader sizingHeader = ProposalSizingHeader.builder()
            .skuProposalHeaderId(proposalId)
            .version(version)
            .isFinalVersion(dto.isFinalVersion() != null ? dto.isFinalVersion() : false)
            .createdBy(userId)
            .createdAt(Instant.now())
            .build();
        sizingHeaderRepository.save(sizingHeader);

        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        proposalHeaderRepository.save(header);

        return toResponse(header);
    }

    // ─── GET SIZING HEADERS ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProposalResponse.SizingHeaderSummary> getSizingHeaders(Long proposalId) {
        List<ProposalSizingHeader> headers = sizingHeaderRepository.findBySkuProposalHeaderId(proposalId);
        return headers.stream()
            .map(this::toSizingHeaderSummary)
            .toList();
    }

    // ─── UPDATE SIZING HEADER ───────────────────────────────────────────────

    public ProposalResponse updateSizingHeader(Long headerId, UpdateSizingHeaderRequest dto, Long userId) {
        ProposalSizingHeader sizingHeader = sizingHeaderRepository.findById(headerId)
            .orElseThrow(() -> new IllegalArgumentException("SizingHeader not found: " + headerId));

        SKUProposalHeader header = proposalHeaderRepository.findById(sizingHeader.getSkuProposalHeaderId())
            .orElseThrow(() -> new ProposalNotFoundException(sizingHeader.getSkuProposalHeaderId().toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }

        if (dto.isFinalVersion() != null) {
            if (dto.isFinalVersion()) {
                // Unset final for other headers
                List<ProposalSizingHeader> others = sizingHeaderRepository.findBySkuProposalHeaderId(header.getId())
                    .stream()
                    .filter(h -> !h.getId().equals(headerId))
                    .toList();
                for (ProposalSizingHeader other : others) {
                    other.setIsFinalVersion(false);
                }
                sizingHeaderRepository.saveAll(others);
            }
            sizingHeader.setIsFinalVersion(dto.isFinalVersion());
        }

        sizingHeader.setUpdatedBy(userId);
        sizingHeader.setUpdatedAt(Instant.now());
        sizingHeaderRepository.save(sizingHeader);

        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        proposalHeaderRepository.save(header);

        return toResponse(header);
    }

    // ─── DELETE SIZING HEADER ───────────────────────────────────────────────

    public ProposalResponse deleteSizingHeader(Long headerId, Long userId) {
        ProposalSizingHeader sizingHeader = sizingHeaderRepository.findById(headerId)
            .orElseThrow(() -> new IllegalArgumentException("SizingHeader not found: " + headerId));

        SKUProposalHeader header = proposalHeaderRepository.findById(sizingHeader.getSkuProposalHeaderId())
            .orElseThrow(() -> new ProposalNotFoundException(sizingHeader.getSkuProposalHeaderId().toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }

        long count = sizingHeaderRepository.countBySkuProposalHeaderId(header.getId());
        if (count <= 1) {
            throw new IllegalStateException("At least 1 sizing header must remain");
        }

        sizingRepository.deleteByProposalSizingHeaderId(headerId);
        sizingHeaderRepository.delete(sizingHeader);

        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        proposalHeaderRepository.save(header);

        return toResponse(header);
    }

    // ─── CREATE SIZINGS ──────────────────────────────────────────────────────

    public ProposalResponse createSizings(Long headerId, CreateSizingRequest dto, Long userId) {
        ProposalSizingHeader sizingHeader = sizingHeaderRepository.findById(headerId)
            .orElseThrow(() -> new IllegalArgumentException("SizingHeader not found: " + headerId));

        SKUProposalHeader header = proposalHeaderRepository.findById(sizingHeader.getSkuProposalHeaderId())
            .orElseThrow(() -> new ProposalNotFoundException(sizingHeader.getSkuProposalHeaderId().toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }

        for (ProposalSizingDto sDto : dto.sizings()) {
            ProposalSizing sizing = ProposalSizing.builder()
                .proposalSizingHeaderId(headerId)
                .skuProposalId(Long.parseLong(sDto.getSkuProposalId()))
                .subcategorySizeId(Long.parseLong(sDto.getSubcategorySizeId()))
                .actualSalesmixPct(sDto.getActualSalesmixPct())
                .actualStPct(sDto.getActualStPct())
                .proposalQuantity(sDto.getProposalQuantity())
                .createdBy(userId)
                .createdAt(Instant.now())
                .build();
            sizingRepository.save(sizing);
        }

        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        proposalHeaderRepository.save(header);

        return toResponse(header);
    }

    // ─── GET SIZINGS ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProposalResponse.SizingSummary> getSizings(Long headerId) {
        List<ProposalSizing> sizings = sizingRepository.findByProposalSizingHeaderId(headerId);
        return sizings.stream()
            .map(s -> ProposalResponse.SizingSummary.builder()
                .id(s.getId())
                .skuProposalId(s.getSkuProposalId())
                .subcategorySizeId(s.getSubcategorySizeId())
                .actualSalesmixPct(s.getActualSalesmixPct())
                .actualStPct(s.getActualStPct())
                .proposalQuantity(s.getProposalQuantity())
                .build())
            .toList();
    }

    // ─── UPDATE SIZING ───────────────────────────────────────────────────────

    public ProposalResponse updateSizing(Long sizingId, UpdateSizingRequest dto, Long userId) {
        ProposalSizing sizing = sizingRepository.findById(sizingId)
            .orElseThrow(() -> new IllegalArgumentException("ProposalSizing not found: " + sizingId));

        ProposalSizingHeader sizingHeader = sizingHeaderRepository.findById(sizing.getProposalSizingHeaderId())
            .orElseThrow(() -> new IllegalArgumentException("SizingHeader not found: " + sizing.getProposalSizingHeaderId()));

        SKUProposalHeader header = proposalHeaderRepository.findById(sizingHeader.getSkuProposalHeaderId())
            .orElseThrow(() -> new ProposalNotFoundException(sizingHeader.getSkuProposalHeaderId().toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }

        if (dto.actualSalesmixPct() != null) sizing.setActualSalesmixPct(dto.actualSalesmixPct());
        if (dto.actualStPct() != null) sizing.setActualStPct(dto.actualStPct());
        if (dto.proposalQuantity() != null) sizing.setProposalQuantity(dto.proposalQuantity());

        sizing.setUpdatedBy(userId);
        sizing.setUpdatedAt(Instant.now());
        sizingRepository.save(sizing);

        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        proposalHeaderRepository.save(header);

        return toResponse(header);
    }

    // ─── DELETE SIZING ──────────────────────────────────────────────────────

    public ProposalResponse deleteSizing(Long sizingId, Long userId) {
        ProposalSizing sizing = sizingRepository.findById(sizingId)
            .orElseThrow(() -> new IllegalArgumentException("ProposalSizing not found: " + sizingId));

        Long headerId = sizing.getProposalSizingHeaderId();
        ProposalSizingHeader sizingHeader = sizingHeaderRepository.findById(headerId)
            .orElseThrow(() -> new IllegalArgumentException("SizingHeader not found: " + headerId));

        SKUProposalHeader header = proposalHeaderRepository.findById(sizingHeader.getSkuProposalHeaderId())
            .orElseThrow(() -> new ProposalNotFoundException(sizingHeader.getSkuProposalHeaderId().toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }

        sizingRepository.delete(sizing);

        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        proposalHeaderRepository.save(header);

        return toResponse(header);
    }

    // ─── SUBMIT ───────────────────────────────────────────────────────────────

    public SKUProposalHeader submit(Long id, Long userId) {
        SKUProposalHeader header = proposalHeaderRepository.findById(id)
            .orElseThrow(() -> new ProposalNotFoundException(id.toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Cannot submit proposal with status: " + header.getStatus());
        }

        header.setStatus("SUBMITTED");
        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        return proposalHeaderRepository.save(header);
    }

    // ─── APPROVE BY LEVEL ───────────────────────────────────────────────────

    public SKUProposalHeader approveByLevel(Long id, Integer level, Long userId) {
        SKUProposalHeader header = proposalHeaderRepository.findById(id)
            .orElseThrow(() -> new ProposalNotFoundException(id.toString()));

        if (!"SUBMITTED".equals(header.getStatus())) {
            throw new IllegalStateException("Cannot approve proposal with status: " + header.getStatus() + ". Must be SUBMITTED.");
        }

        header.setStatus("APPROVED");
        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        return proposalHeaderRepository.save(header);
    }

    // ─── REJECT ───────────────────────────────────────────────────────────────

    public SKUProposalHeader reject(Long id, Long userId) {
        SKUProposalHeader header = proposalHeaderRepository.findById(id)
            .orElseThrow(() -> new ProposalNotFoundException(id.toString()));

        if (!"SUBMITTED".equals(header.getStatus())) {
            throw new IllegalStateException("Cannot reject proposal with status: " + header.getStatus() + ". Must be SUBMITTED.");
        }

        header.setStatus("REJECTED");
        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        return proposalHeaderRepository.save(header);
    }

    // ─── DELETE ────────────────────────────────────────────────────────────────

    public void remove(Long id) {
        SKUProposalHeader header = proposalHeaderRepository.findById(id)
            .orElseThrow(() -> new ProposalNotFoundException(id.toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be deleted");
        }

        // Cascade delete children
        List<SKUProposal> proposals = proposalRepository.findBySkuProposalHeaderId(id);
        for (SKUProposal p : proposals) {
            allocateRepository.deleteBySkuProposalId(p.getId());
        }
        proposalRepository.deleteBySkuProposalHeaderId(id);

        List<ProposalSizingHeader> sizingHeaders = sizingHeaderRepository.findBySkuProposalHeaderId(id);
        for (ProposalSizingHeader sh : sizingHeaders) {
            sizingRepository.deleteByProposalSizingHeaderId(sh.getId());
        }
        sizingHeaderRepository.deleteBySkuProposalHeaderId(id);

        proposalHeaderRepository.delete(header);
    }

    // ─── STATISTICS ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProposalStatisticsResponse getStatistics() {
        List<SKUProposalHeader> headers = proposalHeaderRepository.findAll();

        long total = headers.size();
        Map<String, Long> byStatus = headers.stream()
            .collect(Collectors.groupingBy(
                h -> h.getStatus() != null ? h.getStatus() : "UNKNOWN",
                Collectors.counting()
            ));

        return ProposalStatisticsResponse.builder()
            .totalProposals(total)
            .byStatus(byStatus)
            .build();
    }

    // ─── MAPPING ──────────────────────────────────────────────────────────────

    private ProposalResponse toResponse(SKUProposalHeader header) {
        ProposalResponse.CreatorSummary creatorSummary = null;
        if (header.getCreator() != null) {
            User c = header.getCreator();
            creatorSummary = ProposalResponse.CreatorSummary.builder()
                .id(c.getId())
                .name(c.getName())
                .email(c.getEmail())
                .build();
        }

        ProposalResponse.AllocateHeaderSummary allocateHeaderSummary = null;
        if (header.getAllocateHeader() != null) {
            AllocateHeader ah = header.getAllocateHeader();
            allocateHeaderSummary = ProposalResponse.AllocateHeaderSummary.builder()
                .id(ah.getId())
                .version(ah.getVersion())
                .brandName(ah.getBrand() != null ? ah.getBrand().getName() : null)
                .brandCode(ah.getBrand() != null ? ah.getBrand().getCode() : null)
                .build();
        }

        List<SKUProposal> proposals = proposalRepository.findBySkuProposalHeaderId(header.getId());
        List<ProposalResponse.SKUProposalSummary> proposalSummaries = new ArrayList<>();
        for (SKUProposal p : proposals) {
            List<SKUAllocate> allocs = allocateRepository.findBySkuProposalId(p.getId());
            List<ProposalResponse.AllocateSummary> allocSummaries = allocs.stream()
                .map(a -> ProposalResponse.AllocateSummary.builder()
                    .id(a.getId())
                    .storeId(a.getStoreId())
                    .quantity(a.getQuantity())
                    .build())
                .toList();

            proposalSummaries.add(ProposalResponse.SKUProposalSummary.builder()
                .id(p.getId())
                .productId(p.getProductId())
                .customerTarget(p.getCustomerTarget())
                .unitCost(p.getUnitCost())
                .srp(p.getSrp())
                .allocations(allocSummaries)
                .build());
        }

        List<ProposalSizingHeader> sizingHeaders = sizingHeaderRepository.findBySkuProposalHeaderId(header.getId());
        List<ProposalResponse.SizingHeaderSummary> sizingHeaderSummaries = sizingHeaders.stream()
            .map(this::toSizingHeaderSummary)
            .toList();

        return ProposalResponse.builder()
            .id(header.getId())
            .status(header.getStatus())
            .isFinalVersion(header.getIsFinalVersion())
            .version(header.getVersion())
            .createdAt(header.getCreatedAt())
            .updatedAt(header.getUpdatedAt())
            .creator(creatorSummary)
            .allocateHeader(allocateHeaderSummary)
            .proposals(proposalSummaries)
            .sizingHeaders(sizingHeaderSummaries)
            .build();
    }

    private ProposalResponse.SizingHeaderSummary toSizingHeaderSummary(ProposalSizingHeader sh) {
        List<ProposalSizing> sizings = sizingRepository.findByProposalSizingHeaderId(sh.getId());
        List<ProposalResponse.SizingSummary> sizingSummaries = sizings.stream()
            .map(s -> ProposalResponse.SizingSummary.builder()
                .id(s.getId())
                .skuProposalId(s.getSkuProposalId())
                .subcategorySizeId(s.getSubcategorySizeId())
                .actualSalesmixPct(s.getActualSalesmixPct())
                .actualStPct(s.getActualStPct())
                .proposalQuantity(s.getProposalQuantity())
                .build())
            .toList();

        return ProposalResponse.SizingHeaderSummary.builder()
            .id(sh.getId())
            .version(sh.getVersion())
            .isFinalVersion(sh.getIsFinalVersion())
            .sizings(sizingSummaries)
            .build();
    }
}
