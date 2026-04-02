package com.vieterp.otb.budget;

import com.vieterp.otb.budget.domain.dto.*;
import com.vieterp.otb.budget.exception.BudgetNotFoundException;
import com.vieterp.otb.budget.repository.BudgetRepository;
import com.vieterp.otb.domain.*;
import com.vieterp.otb.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final AllocateHeaderRepository allocateHeaderRepository;
    private final BudgetAllocateRepository budgetAllocateRepository;
    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final SeasonGroupRepository seasonGroupRepository;
    private final SeasonRepository seasonRepository;

    // ─── LIST ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> findAll(Integer fiscalYear, String status, int page, int pageSize) {
        Specification<Budget> spec = Specification.where(null);

        if (fiscalYear != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("fiscalYear"), fiscalYear));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), status));
        }

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Budget> pageResult = budgetRepository.findAll(spec, pageable);

        List<BudgetResponse> data = pageResult.getContent().stream()
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
    public BudgetResponse findById(Long id) {
        Budget budget = budgetRepository.findByIdWithAllocations(id)
            .orElseThrow(() -> new BudgetNotFoundException(id.toString()));
        return toResponse(budget);
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    public BudgetResponse create(CreateBudgetRequest dto, Long userId) {
        User creator = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Budget budget = Budget.builder()
            .name(dto.name())
            .amount(dto.amount())
            .description(dto.description())
            .fiscalYear(dto.fiscalYear())
            .status("DRAFT")
            .creator(creator)
            .createdAt(Instant.now())
            .build();
        budget = budgetRepository.save(budget);

        if (dto.brandId() != null) {
            Brand brand = brandRepository.findById(Long.parseLong(dto.brandId()))
                .orElseThrow(() -> new IllegalArgumentException("Brand not found: " + dto.brandId()));

            if (dto.allocations() != null && !dto.allocations().isEmpty()) {
                createAllocateHeader(budget, brand, dto.allocations(), creator, dto.isFinalVersion() != null && dto.isFinalVersion());
            } else {
                int version = allocateHeaderRepository
                    .findLatestByBudgetAndBrand(budget.getId(), brand.getId())
                    .map(h -> h.getVersion() + 1)
                    .orElse(1);

                AllocateHeader header = AllocateHeader.builder()
                    .budget(budget)
                    .brand(brand)
                    .version(version)
                    .isFinalVersion(false)
                    .isSnapshot(false)
                    .creator(creator)
                    .createdAt(Instant.now())
                    .build();
                allocateHeaderRepository.save(header);
            }
        }

        return findById(budget.getId());
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    public BudgetResponse update(Long id, UpdateBudgetRequest dto, Long userId) {
        Budget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new BudgetNotFoundException(id.toString()));

        if (!"DRAFT".equals(budget.getStatus())) {
            throw new IllegalStateException("Only draft budgets can be edited");
        }

        if (dto.name() != null) budget.setName(dto.name());
        if (dto.amount() != null) budget.setAmount(dto.amount());
        if (dto.description() != null) budget.setDescription(dto.description());

        budget.setUpdater(userRepository.getReferenceById(userId));
        budget.setUpdatedAt(Instant.now());
        budget = budgetRepository.save(budget);

        return findById(budget.getId());
    }

    // ─── CREATE ALLOCATE HEADER ───────────────────────────────────────────────

    public BudgetResponse createAllocateHeader(Long budgetId, CreateAllocateRequest dto, Long userId) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new BudgetNotFoundException(budgetId.toString()));
        Brand brand = brandRepository.findById(Long.parseLong(dto.brandId()))
            .orElseThrow(() -> new IllegalArgumentException("Brand not found: " + dto.brandId()));
        User creator = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<BudgetAllocateDto> valid = dto.allocations().stream()
            .filter(a -> a.getBudgetAmount() != null && a.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0)
            .toList();
        if (valid.isEmpty()) {
            throw new IllegalArgumentException("At least one allocation must have amount > 0");
        }

        createAllocateHeader(budget, brand, dto.allocations(), creator, dto.isFinalVersion() != null && dto.isFinalVersion());
        return findById(budgetId);
    }

    private AllocateHeader createAllocateHeader(Budget budget, Brand brand,
            List<BudgetAllocateDto> allocations, User creator, boolean isFinalVersion) {

        int version = allocateHeaderRepository
            .findLatestByBudgetAndBrand(budget.getId(), brand.getId())
            .map(h -> h.getVersion() + 1)
            .orElse(1);

        AllocateHeader header = AllocateHeader.builder()
            .budget(budget)
            .brand(brand)
            .version(version)
            .isFinalVersion(isFinalVersion)
            .isSnapshot(false)
            .creator(creator)
            .createdAt(Instant.now())
            .budgetAllocates(new ArrayList<>())
            .build();

        for (BudgetAllocateDto alloc : allocations) {
            BudgetAllocate ba = BudgetAllocate.builder()
                .allocateHeader(header)
                .store(storeRepository.getReferenceById(Long.parseLong(alloc.getStoreId())))
                .seasonGroup(seasonGroupRepository.getReferenceById(Long.parseLong(alloc.getSeasonGroupId())))
                .season(seasonRepository.getReferenceById(Long.parseLong(alloc.getSeasonId())))
                .budgetAmount(alloc.getBudgetAmount())
                .creator(creator)
                .createdAt(Instant.now())
                .build();
            header.getBudgetAllocates().add(ba);
        }

        return allocateHeaderRepository.save(header);
    }

    // ─── UPDATE ALLOCATE HEADER ───────────────────────────────────────────────

    public BudgetResponse updateAllocateHeader(Long headerId, UpdateAllocateRequest dto, Long userId) {
        AllocateHeader header = allocateHeaderRepository.findById(headerId)
            .orElseThrow(() -> new IllegalArgumentException("Allocate header not found: " + headerId));
        User updater = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (dto.isFinalVersion() != null) {
            header.setIsFinalVersion(dto.isFinalVersion());
        }

        // Replace allocations
        budgetAllocateRepository.deleteByAllocateHeaderId(headerId);
        header.getBudgetAllocates().clear();

        for (BudgetAllocateDto alloc : dto.allocations()) {
            BudgetAllocate ba = BudgetAllocate.builder()
                .allocateHeader(header)
                .store(storeRepository.getReferenceById(Long.parseLong(alloc.getStoreId())))
                .seasonGroup(seasonGroupRepository.getReferenceById(Long.parseLong(alloc.getSeasonGroupId())))
                .season(seasonRepository.getReferenceById(Long.parseLong(alloc.getSeasonId())))
                .budgetAmount(alloc.getBudgetAmount())
                .creator(updater)
                .createdAt(Instant.now())
                .build();
            header.getBudgetAllocates().add(ba);
        }

        header.setUpdater(updater);
        header.setUpdatedAt(Instant.now());
        allocateHeaderRepository.save(header);

        return findById(header.getBudget().getId());
    }

    // ─── SUBMIT ───────────────────────────────────────────────────────────────

    public Budget submit(Long id, Long userId) {
        Budget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new BudgetNotFoundException(id.toString()));

        if (!"DRAFT".equals(budget.getStatus())) {
            throw new IllegalStateException("Cannot submit budget with status: " + budget.getStatus());
        }

        budget.setStatus("SUBMITTED");
        budget.setUpdater(userRepository.getReferenceById(userId));
        budget.setUpdatedAt(Instant.now());
        return budgetRepository.save(budget);
    }

    // ─── APPROVE ──────────────────────────────────────────────────────────────

    public Budget approve(Long id, Long userId) {
        Budget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new BudgetNotFoundException(id.toString()));

        if (!"SUBMITTED".equals(budget.getStatus())) {
            throw new IllegalStateException("Cannot approve budget with status: " + budget.getStatus() + ". Must be SUBMITTED.");
        }

        budget.setStatus("APPROVED");
        budget.setUpdater(userRepository.getReferenceById(userId));
        budget.setUpdatedAt(Instant.now());
        return budgetRepository.save(budget);
    }

    // ─── REJECT ────────────────────────────────────────────────────────────────

    public Budget reject(Long id, Long userId) {
        Budget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new BudgetNotFoundException(id.toString()));

        if (!"SUBMITTED".equals(budget.getStatus())) {
            throw new IllegalStateException("Cannot reject budget with status: " + budget.getStatus() + ". Must be SUBMITTED.");
        }

        budget.setStatus("REJECTED");
        budget.setUpdater(userRepository.getReferenceById(userId));
        budget.setUpdatedAt(Instant.now());
        return budgetRepository.save(budget);
    }

    // ─── DELETE ────────────────────────────────────────────────────────────────

    public void remove(Long id) {
        Budget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new BudgetNotFoundException(id.toString()));

        if (!"DRAFT".equals(budget.getStatus())) {
            throw new IllegalStateException("Only draft budgets can be deleted");
        }

        budgetRepository.delete(budget);
    }

    // ─── ARCHIVE ───────────────────────────────────────────────────────────────

    public Budget archive(Long id) {
        Budget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new BudgetNotFoundException(id.toString()));

        if (!"APPROVED".equals(budget.getStatus())) {
            throw new IllegalStateException("Only approved budgets can be archived. Current status: " + budget.getStatus());
        }

        budget.setStatus("ARCHIVED");
        budget.setUpdatedAt(Instant.now());
        return budgetRepository.save(budget);
    }

    // ─── SET FINAL VERSION ─────────────────────────────────────────────────────

    public BudgetResponse setFinalVersion(Long headerId) {
        AllocateHeader header = allocateHeaderRepository.findById(headerId)
            .orElseThrow(() -> new IllegalArgumentException("Allocate header not found: " + headerId));

        // Unset final for all other headers of same brand + budget
        List<AllocateHeader> others = allocateHeaderRepository.findOthersByBudgetAndBrand(
            header.getBudget().getId(), header.getBrand().getId(), headerId);
        for (AllocateHeader other : others) {
            other.setIsFinalVersion(false);
        }
        allocateHeaderRepository.saveAll(others);

        header.setIsFinalVersion(true);
        allocateHeaderRepository.save(header);

        return findById(header.getBudget().getId());
    }

    // ─── STATISTICS ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BudgetStatisticsResponse getStatistics(Integer fiscalYear) {
        Specification<Budget> spec = Specification.where(null);
        if (fiscalYear != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("fiscalYear"), fiscalYear));
        }

        List<Budget> budgets = budgetRepository.findAll(spec);

        long total = budgets.size();
        BigDecimal totalAmount = budgets.stream()
            .map(Budget::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal approvedAmount = budgets.stream()
            .filter(b -> "APPROVED".equals(b.getStatus()))
            .map(Budget::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> byStatus = budgets.stream()
            .collect(Collectors.groupingBy(
                b -> b.getStatus() != null ? b.getStatus() : "UNKNOWN",
                Collectors.counting()
            ));

        return BudgetStatisticsResponse.builder()
            .totalBudgets(total)
            .totalAmount(totalAmount)
            .approvedAmount(approvedAmount)
            .byStatus(byStatus)
            .build();
    }

    // ─── MAPPING ──────────────────────────────────────────────────────────────

    private BudgetResponse toResponse(Budget budget) {
        List<BudgetResponse.AllocateHeaderSummary> headerSummaries = new ArrayList<>();

        if (budget.getAllocateHeaders() != null) {
            for (AllocateHeader ah : budget.getAllocateHeaders()) {
                if (Boolean.TRUE.equals(ah.getIsSnapshot())) continue;

                BudgetResponse.BrandSummary brandSummary = null;
                if (ah.getBrand() != null) {
                    Brand b = ah.getBrand();
                    brandSummary = BudgetResponse.BrandSummary.builder()
                        .id(b.getId())
                        .code(b.getCode())
                        .name(b.getName())
                        .groupBrandName(b.getGroupBrand() != null ? b.getGroupBrand().getName() : null)
                        .build();
                }

                List<BudgetResponse.BudgetAllocateSummary> allocateSummaries = new ArrayList<>();
                if (ah.getBudgetAllocates() != null) {
                    for (BudgetAllocate ba : ah.getBudgetAllocates()) {
                        allocateSummaries.add(BudgetResponse.BudgetAllocateSummary.builder()
                            .id(ba.getId())
                            .storeId(ba.getStore() != null ? ba.getStore().getId() : null)
                            .seasonGroupId(ba.getSeasonGroup() != null ? ba.getSeasonGroup().getId() : null)
                            .seasonId(ba.getSeason() != null ? ba.getSeason().getId() : null)
                            .budgetAmount(ba.getBudgetAmount())
                            .build());
                    }
                }

                headerSummaries.add(BudgetResponse.AllocateHeaderSummary.builder()
                    .id(ah.getId())
                    .version(ah.getVersion())
                    .isFinalVersion(ah.getIsFinalVersion())
                    .isSnapshot(ah.getIsSnapshot())
                    .brand(brandSummary)
                    .budgetAllocates(allocateSummaries)
                    .build());
            }
        }

        headerSummaries.sort((a, b) -> Integer.compare(
            b.version() != null ? b.version() : 0,
            a.version() != null ? a.version() : 0
        ));

        BudgetResponse.CreatorSummary creatorSummary = null;
        if (budget.getCreator() != null) {
            User c = budget.getCreator();
            creatorSummary = BudgetResponse.CreatorSummary.builder()
                .id(c.getId())
                .name(c.getName())
                .email(c.getEmail())
                .build();
        }

        return BudgetResponse.builder()
            .id(budget.getId())
            .name(budget.getName())
            .amount(budget.getAmount())
            .description(budget.getDescription())
            .status(budget.getStatus())
            .fiscalYear(budget.getFiscalYear())
            .createdAt(budget.getCreatedAt())
            .updatedAt(budget.getUpdatedAt())
            .creator(creatorSummary)
            .allocateHeaders(headerSummaries)
            .build();
    }
}
