package com.vieterp.otb.planning;

import com.vieterp.otb.domain.*;
import com.vieterp.otb.domain.repository.*;
import com.vieterp.otb.planning.dto.*;
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
public class PlanningService {

    private final PlanningHeaderRepository planningHeaderRepository;
    private final AllocateHeaderRepository allocateHeaderRepository;
    private final PlanningCollectionRepository planningCollectionRepository;
    private final PlanningGenderRepository planningGenderRepository;
    private final PlanningCategoryRepository planningCategoryRepository;
    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final GenderRepository genderRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final SeasonTypeRepository seasonTypeRepository;
    private final SeasonGroupRepository seasonGroupRepository;
    private final SeasonRepository seasonRepository;
    private final BudgetAllocateRepository budgetAllocateRepository;

    // ─── LIST ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> findAll(
            Integer page,
            Integer pageSize,
            String status,
            String budgetId,
            String brandId,
            String allocateHeaderId) {

        Specification<PlanningHeader> spec = Specification.where(null);

        if (status != null && !status.isBlank()) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), status));
        }
        if (allocateHeaderId != null && !allocateHeaderId.isBlank()) {
            spec = spec.and((root, cq, cb) ->
                cb.equal(root.get("allocateHeader").get("id"), Long.parseLong(allocateHeaderId)));
        }
        if (brandId != null && !brandId.isBlank()) {
            spec = spec.and((root, cq, cb) ->
                cb.equal(root.get("allocateHeader").get("brand").get("id"), Long.parseLong(brandId)));
        }
        // Always exclude snapshot records
        spec = spec.and((root, cq, cb) ->
            cb.equal(root.get("allocateHeader").get("isSnapshot"), false));

        Pageable pageable = PageRequest.of(
            (page != null ? page - 1 : 0),
            (pageSize != null ? pageSize : 20),
            Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PlanningHeader> pageResult = planningHeaderRepository.findAll(spec, pageable);

        List<PlanningResponse.HeaderSummary> data = pageResult.getContent().stream()
            .map(this::toHeaderSummary)
            .toList();

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", page != null ? page : 1);
        meta.put("pageSize", pageSize != null ? pageSize : 20);
        meta.put("total", pageResult.getTotalElements());
        meta.put("totalPages", pageResult.getTotalPages());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("meta", meta);
        return result;
    }

    // ─── GET ONE ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PlanningResponse findOne(Long id) {
        PlanningHeader header = planningHeaderRepository.findByIdWithHeader(id)
            .orElseThrow(() -> new PlanningNotFoundException(id.toString()));

        List<PlanningCollection> collections =
            planningCollectionRepository.findByPlanningHeaderId(id);
        List<PlanningGender> genders =
            planningGenderRepository.findByPlanningHeaderId(id);
        List<PlanningCategory> categories =
            planningCategoryRepository.findByPlanningHeaderId(id);

        return toResponse(header, collections, genders, categories);
    }

    // ─── HISTORICAL ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PlanningResponse findHistorical(Integer fiscalYear, String seasonGroupName,
            String seasonName, String brandId) {

        Long brandIdLong = Long.parseLong(brandId);

        // Step 1: Find AllocateHeaders matching brand + fiscal_year
        //         AND whose budget_allocates touch the target season_group + season
        List<AllocateHeader> matchingHeaders = allocateHeaderRepository.findAll(
            (root, cq, cb) -> {
                var brandJoin = cb.equal(root.get("brand").get("id"), brandIdLong);
                var snapshotJoin = cb.equal(root.get("isSnapshot"), false);
                var fiscalJoin = cb.equal(root.get("budget").get("fiscalYear"), fiscalYear);
                return cb.and(brandJoin, snapshotJoin, fiscalJoin);
            });

        // Filter those that have budget_allocates with matching season_group + season
        List<AllocateHeader> filtered = matchingHeaders.stream()
            .filter(ah -> ah.getBudgetAllocates() != null && ah.getBudgetAllocates().stream()
                .anyMatch(ba ->
                    ba.getSeasonGroup() != null &&
                    seasonGroupName.equals(ba.getSeasonGroup().getName()) &&
                    ba.getSeason() != null &&
                    seasonName.equals(ba.getSeason().getName())))
            .toList();

        if (filtered.isEmpty()) return null;

        // Step 2: Find best PlanningHeader (prefer final, then most recent approved)
        List<PlanningHeader> candidates = new ArrayList<>();
        for (AllocateHeader ah : filtered) {
            candidates.addAll(planningHeaderRepository.findByAllocateHeaderIdOrderByVersionDesc(ah.getId()));
        }

        PlanningHeader best = candidates.stream()
            .filter(ph -> Boolean.TRUE.equals(ph.getIsFinalVersion()))
            .findFirst()
            .orElseGet(() -> candidates.stream()
                .filter(ph -> "APPROVED".equals(ph.getStatus()))
                .findFirst()
                .orElse(candidates.stream().findFirst().orElse(null)));

        if (best == null) return null;

        return findOne(best.getId());
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    public PlanningResponse create(CreatePlanningRequest dto, Long userId) {
        AllocateHeader allocateHeader = allocateHeaderRepository.findById(Long.parseLong(dto.allocateHeaderId()))
            .orElseThrow(() -> new IllegalArgumentException("AllocateHeader not found: " + dto.allocateHeaderId()));

        User creator = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Version = max version for this brand + 1
        List<PlanningHeader> existing = planningHeaderRepository
            .findByBrandIdOrderByVersionDesc(allocateHeader.getBrand().getId());
        int version = existing.isEmpty() ? 1 :
            existing.get(0).getVersion() != null ? existing.get(0).getVersion() + 1 : 1;

        PlanningHeader header = PlanningHeader.builder()
            .allocateHeader(allocateHeader)
            .version(version)
            .status("DRAFT")
            .isFinalVersion(false)
            .creator(creator)
            .createdAt(Instant.now())
            .build();
        header = planningHeaderRepository.save(header);

        // Bulk-insert collections
        if (dto.seasonTypes() != null && !dto.seasonTypes().isEmpty()) {
            for (PlanningCollectionDto cdto : dto.seasonTypes()) {
                PlanningCollection pc = PlanningCollection.builder()
                    .seasonTypeId(Long.parseLong(cdto.seasonTypeId()))
                    .storeId(Long.parseLong(cdto.storeId()))
                    .planningHeaderId(header.getId())
                    .actualBuyPct(cdto.actualBuyPct() != null ? cdto.actualBuyPct() : BigDecimal.ZERO)
                    .actualSalesPct(cdto.actualSalesPct() != null ? cdto.actualSalesPct() : BigDecimal.ZERO)
                    .actualStPct(cdto.actualStPct() != null ? cdto.actualStPct() : BigDecimal.ZERO)
                    .actualMoc(cdto.actualMoc() != null ? cdto.actualMoc() : BigDecimal.ZERO)
                    .proposedBuyPct(cdto.proposedBuyPct() != null ? cdto.proposedBuyPct() : BigDecimal.ZERO)
                    .otbProposedAmount(cdto.otbProposedAmount() != null ? cdto.otbProposedAmount() : BigDecimal.ZERO)
                    .pctVarVsLast(cdto.pctVarVsLast() != null ? cdto.pctVarVsLast() : BigDecimal.ZERO)
                    .createdBy(userId)
                    .createdAt(Instant.now())
                    .build();
                planningCollectionRepository.save(pc);
            }
        }

        // Bulk-insert genders
        if (dto.genders() != null && !dto.genders().isEmpty()) {
            for (PlanningGenderDto gdto : dto.genders()) {
                PlanningGender pg = PlanningGender.builder()
                    .genderId(Long.parseLong(gdto.genderId()))
                    .storeId(Long.parseLong(gdto.storeId()))
                    .planningHeaderId(header.getId())
                    .actualBuyPct(gdto.actualBuyPct() != null ? gdto.actualBuyPct() : BigDecimal.ZERO)
                    .actualSalesPct(gdto.actualSalesPct() != null ? gdto.actualSalesPct() : BigDecimal.ZERO)
                    .actualStPct(gdto.actualStPct() != null ? gdto.actualStPct() : BigDecimal.ZERO)
                    .proposedBuyPct(gdto.proposedBuyPct() != null ? gdto.proposedBuyPct() : BigDecimal.ZERO)
                    .otbProposedAmount(gdto.otbProposedAmount() != null ? gdto.otbProposedAmount() : BigDecimal.ZERO)
                    .pctVarVsLast(gdto.pctVarVsLast() != null ? gdto.pctVarVsLast() : BigDecimal.ZERO)
                    .createdBy(userId)
                    .createdAt(Instant.now())
                    .build();
                planningGenderRepository.save(pg);
            }
        }

        // Bulk-insert categories
        if (dto.categories() != null && !dto.categories().isEmpty()) {
            for (PlanningCategoryDto cdto : dto.categories()) {
                PlanningCategory pc = PlanningCategory.builder()
                    .subcategoryId(Long.parseLong(cdto.subcategoryId()))
                    .planningHeaderId(header.getId())
                    .actualBuyPct(cdto.actualBuyPct() != null ? cdto.actualBuyPct() : BigDecimal.ZERO)
                    .actualSalesPct(cdto.actualSalesPct() != null ? cdto.actualSalesPct() : BigDecimal.ZERO)
                    .actualStPct(cdto.actualStPct() != null ? cdto.actualStPct() : BigDecimal.ZERO)
                    .proposedBuyPct(cdto.proposedBuyPct() != null ? cdto.proposedBuyPct() : BigDecimal.ZERO)
                    .otbProposedAmount(cdto.otbProposedAmount() != null ? cdto.otbProposedAmount() : BigDecimal.ZERO)
                    .varLastyearPct(cdto.varLastyearPct() != null ? cdto.varLastyearPct() : BigDecimal.ZERO)
                    .otbActualAmount(cdto.otbActualAmount() != null ? cdto.otbActualAmount() : BigDecimal.ZERO)
                    .otbActualBuyPct(cdto.otbActualBuyPct() != null ? cdto.otbActualBuyPct() : BigDecimal.ZERO)
                    .createdBy(userId)
                    .createdAt(Instant.now())
                    .build();
                planningCategoryRepository.save(pc);
            }
        }

        return findOne(header.getId());
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    public PlanningResponse update(Long id, UpdatePlanningRequest dto, Long userId) {
        PlanningHeader header = planningHeaderRepository.findById(id)
            .orElseThrow(() -> new PlanningNotFoundException(id.toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft plannings can be edited. Current status: " + header.getStatus());
        }

        if (dto.allocateHeaderId() != null) {
            AllocateHeader newAh = allocateHeaderRepository.findById(Long.parseLong(dto.allocateHeaderId()))
                .orElseThrow(() -> new IllegalArgumentException("AllocateHeader not found: " + dto.allocateHeaderId()));
            header.setAllocateHeader(newAh);
        }

        // Replace collections
        if (dto.seasonTypes() != null) {
            planningCollectionRepository.deleteByPlanningHeaderId(id);
            for (PlanningCollectionDto cdto : dto.seasonTypes()) {
                PlanningCollection pc = PlanningCollection.builder()
                    .seasonTypeId(Long.parseLong(cdto.seasonTypeId()))
                    .storeId(Long.parseLong(cdto.storeId()))
                    .planningHeaderId(id)
                    .actualBuyPct(cdto.actualBuyPct() != null ? cdto.actualBuyPct() : BigDecimal.ZERO)
                    .actualSalesPct(cdto.actualSalesPct() != null ? cdto.actualSalesPct() : BigDecimal.ZERO)
                    .actualStPct(cdto.actualStPct() != null ? cdto.actualStPct() : BigDecimal.ZERO)
                    .actualMoc(cdto.actualMoc() != null ? cdto.actualMoc() : BigDecimal.ZERO)
                    .proposedBuyPct(cdto.proposedBuyPct() != null ? cdto.proposedBuyPct() : BigDecimal.ZERO)
                    .otbProposedAmount(cdto.otbProposedAmount() != null ? cdto.otbProposedAmount() : BigDecimal.ZERO)
                    .pctVarVsLast(cdto.pctVarVsLast() != null ? cdto.pctVarVsLast() : BigDecimal.ZERO)
                    .createdBy(userId)
                    .createdAt(Instant.now())
                    .build();
                planningCollectionRepository.save(pc);
            }
        }

        // Replace genders
        if (dto.genders() != null) {
            planningGenderRepository.deleteByPlanningHeaderId(id);
            for (PlanningGenderDto gdto : dto.genders()) {
                PlanningGender pg = PlanningGender.builder()
                    .genderId(Long.parseLong(gdto.genderId()))
                    .storeId(Long.parseLong(gdto.storeId()))
                    .planningHeaderId(id)
                    .actualBuyPct(gdto.actualBuyPct() != null ? gdto.actualBuyPct() : BigDecimal.ZERO)
                    .actualSalesPct(gdto.actualSalesPct() != null ? gdto.actualSalesPct() : BigDecimal.ZERO)
                    .actualStPct(gdto.actualStPct() != null ? gdto.actualStPct() : BigDecimal.ZERO)
                    .proposedBuyPct(gdto.proposedBuyPct() != null ? gdto.proposedBuyPct() : BigDecimal.ZERO)
                    .otbProposedAmount(gdto.otbProposedAmount() != null ? gdto.otbProposedAmount() : BigDecimal.ZERO)
                    .pctVarVsLast(gdto.pctVarVsLast() != null ? gdto.pctVarVsLast() : BigDecimal.ZERO)
                    .createdBy(userId)
                    .createdAt(Instant.now())
                    .build();
                planningGenderRepository.save(pg);
            }
        }

        // Replace categories
        if (dto.categories() != null) {
            planningCategoryRepository.deleteByPlanningHeaderId(id);
            for (PlanningCategoryDto cdto : dto.categories()) {
                PlanningCategory pc = PlanningCategory.builder()
                    .subcategoryId(Long.parseLong(cdto.subcategoryId()))
                    .planningHeaderId(id)
                    .actualBuyPct(cdto.actualBuyPct() != null ? cdto.actualBuyPct() : BigDecimal.ZERO)
                    .actualSalesPct(cdto.actualSalesPct() != null ? cdto.actualSalesPct() : BigDecimal.ZERO)
                    .actualStPct(cdto.actualStPct() != null ? cdto.actualStPct() : BigDecimal.ZERO)
                    .proposedBuyPct(cdto.proposedBuyPct() != null ? cdto.proposedBuyPct() : BigDecimal.ZERO)
                    .otbProposedAmount(cdto.otbProposedAmount() != null ? cdto.otbProposedAmount() : BigDecimal.ZERO)
                    .varLastyearPct(cdto.varLastyearPct() != null ? cdto.varLastyearPct() : BigDecimal.ZERO)
                    .otbActualAmount(cdto.otbActualAmount() != null ? cdto.otbActualAmount() : BigDecimal.ZERO)
                    .otbActualBuyPct(cdto.otbActualBuyPct() != null ? cdto.otbActualBuyPct() : BigDecimal.ZERO)
                    .createdBy(userId)
                    .createdAt(Instant.now())
                    .build();
                planningCategoryRepository.save(pc);
            }
        }

        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        planningHeaderRepository.save(header);

        return findOne(id);
    }

    // ─── CREATE FROM VERSION (copy) ───────────────────────────────────────────

    public PlanningResponse createFromVersion(Long sourceId, Long userId) {
        PlanningHeader source = planningHeaderRepository.findByIdWithHeader(sourceId)
            .orElseThrow(() -> new PlanningNotFoundException(sourceId.toString()));

        List<PlanningCollection> sourceCollections =
            planningCollectionRepository.findByPlanningHeaderId(sourceId);
        List<PlanningGender> sourceGenders =
            planningGenderRepository.findByPlanningHeaderId(sourceId);
        List<PlanningCategory> sourceCategories =
            planningCategoryRepository.findByPlanningHeaderId(sourceId);

        // Version = max version for this brand + 1
        Long brandId = source.getAllocateHeader() != null ?
            source.getAllocateHeader().getBrand().getId() : null;
        List<PlanningHeader> existing = brandId != null ?
            planningHeaderRepository.findByBrandIdOrderByVersionDesc(brandId) :
            Collections.emptyList();
        int version = existing.isEmpty() ? 1 :
            existing.get(0).getVersion() != null ? existing.get(0).getVersion() + 1 : 1;

        User creator = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        PlanningHeader header = PlanningHeader.builder()
            .allocateHeader(source.getAllocateHeader())
            .version(version)
            .status("DRAFT")
            .isFinalVersion(false)
            .creator(creator)
            .createdAt(Instant.now())
            .build();
        header = planningHeaderRepository.save(header);

        // Copy collections
        for (PlanningCollection sc : sourceCollections) {
            PlanningCollection pc = PlanningCollection.builder()
                .seasonTypeId(sc.getSeasonTypeId())
                .storeId(sc.getStoreId())
                .planningHeaderId(header.getId())
                .actualBuyPct(sc.getActualBuyPct())
                .actualSalesPct(sc.getActualSalesPct())
                .actualStPct(sc.getActualStPct())
                .actualMoc(sc.getActualMoc())
                .proposedBuyPct(sc.getProposedBuyPct())
                .otbProposedAmount(sc.getOtbProposedAmount())
                .pctVarVsLast(sc.getPctVarVsLast())
                .createdBy(userId)
                .createdAt(Instant.now())
                .build();
            planningCollectionRepository.save(pc);
        }

        // Copy genders
        for (PlanningGender sg : sourceGenders) {
            PlanningGender pg = PlanningGender.builder()
                .genderId(sg.getGenderId())
                .storeId(sg.getStoreId())
                .planningHeaderId(header.getId())
                .actualBuyPct(sg.getActualBuyPct())
                .actualSalesPct(sg.getActualSalesPct())
                .actualStPct(sg.getActualStPct())
                .proposedBuyPct(sg.getProposedBuyPct())
                .otbProposedAmount(sg.getOtbProposedAmount())
                .pctVarVsLast(sg.getPctVarVsLast())
                .createdBy(userId)
                .createdAt(Instant.now())
                .build();
            planningGenderRepository.save(pg);
        }

        // Copy categories
        for (PlanningCategory sc : sourceCategories) {
            PlanningCategory pc = PlanningCategory.builder()
                .subcategoryId(sc.getSubcategoryId())
                .planningHeaderId(header.getId())
                .actualBuyPct(sc.getActualBuyPct())
                .actualSalesPct(sc.getActualSalesPct())
                .actualStPct(sc.getActualStPct())
                .proposedBuyPct(sc.getProposedBuyPct())
                .otbProposedAmount(sc.getOtbProposedAmount())
                .varLastyearPct(sc.getVarLastyearPct())
                .otbActualAmount(sc.getOtbActualAmount())
                .otbActualBuyPct(sc.getOtbActualBuyPct())
                .createdBy(userId)
                .createdAt(Instant.now())
                .build();
            planningCategoryRepository.save(pc);
        }

        return findOne(header.getId());
    }

    // ─── SUBMIT ───────────────────────────────────────────────────────────────

    public PlanningHeader submit(Long id, Long userId) {
        PlanningHeader header = planningHeaderRepository.findById(id)
            .orElseThrow(() -> new PlanningNotFoundException(id.toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Cannot submit with status: " + header.getStatus() + ". Must be DRAFT.");
        }

        header.setStatus("SUBMITTED");
        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        return planningHeaderRepository.save(header);
    }

    // ─── APPROVE BY LEVEL ─────────────────────────────────────────────────────

    public PlanningHeader approveByLevel(Long id, String level, String action, String comment, Long userId) {
        PlanningHeader header = planningHeaderRepository.findById(id)
            .orElseThrow(() -> new PlanningNotFoundException(id.toString()));

        if (!"SUBMITTED".equals(header.getStatus())) {
            throw new IllegalStateException("Cannot approve/reject with status: " + header.getStatus() + ". Must be SUBMITTED.");
        }

        String newStatus = "REJECTED".equals(action) ? "REJECTED" : "APPROVED";
        header.setStatus(newStatus);
        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        return planningHeaderRepository.save(header);
    }

    // ─── FINALIZE ─────────────────────────────────────────────────────────────

    public PlanningHeader finalize(Long id, Long userId) {
        PlanningHeader header = planningHeaderRepository.findById(id)
            .orElseThrow(() -> new PlanningNotFoundException(id.toString()));

        header.setIsFinalVersion(true);
        header.setUpdater(userRepository.getReferenceById(userId));
        header.setUpdatedAt(Instant.now());
        return planningHeaderRepository.save(header);
    }

    // ─── UPDATE DETAIL ────────────────────────────────────────────────────────

    public Object updateDetail(Long planningId, Long detailId, Object dtoRaw, Long userId) {
        PlanningHeader header = planningHeaderRepository.findById(planningId)
            .orElseThrow(() -> new PlanningNotFoundException(planningId.toString()));

        @SuppressWarnings("unchecked")
        Map<String, Object> dto = (Map<String, Object>) dtoRaw;

        // Try to find in collections
        Optional<PlanningCollection> colOpt = planningCollectionRepository.findById(detailId)
            .filter(c -> c.getPlanningHeaderId().equals(planningId));
        if (colOpt.isPresent()) {
            PlanningCollection col = colOpt.get();
            if (dto.containsKey("proposedBuyPct"))
                col.setProposedBuyPct(toBigDecimal(dto.get("proposedBuyPct")));
            if (dto.containsKey("otbProposedAmount"))
                col.setOtbProposedAmount(toBigDecimal(dto.get("otbProposedAmount")));
            if (dto.containsKey("actualBuyPct"))
                col.setActualBuyPct(toBigDecimal(dto.get("actualBuyPct")));
            if (dto.containsKey("actualSalesPct"))
                col.setActualSalesPct(toBigDecimal(dto.get("actualSalesPct")));
            if (dto.containsKey("actualStPct"))
                col.setActualStPct(toBigDecimal(dto.get("actualStPct")));
            col.setUpdatedBy(userId);
            col.setUpdatedAt(Instant.now());
            return planningCollectionRepository.save(col);
        }

        // Try to find in genders
        Optional<PlanningGender> genOpt = planningGenderRepository.findById(detailId)
            .filter(g -> g.getPlanningHeaderId().equals(planningId));
        if (genOpt.isPresent()) {
            PlanningGender gen = genOpt.get();
            if (dto.containsKey("proposedBuyPct"))
                gen.setProposedBuyPct(toBigDecimal(dto.get("proposedBuyPct")));
            if (dto.containsKey("otbProposedAmount"))
                gen.setOtbProposedAmount(toBigDecimal(dto.get("otbProposedAmount")));
            if (dto.containsKey("actualBuyPct"))
                gen.setActualBuyPct(toBigDecimal(dto.get("actualBuyPct")));
            if (dto.containsKey("actualSalesPct"))
                gen.setActualSalesPct(toBigDecimal(dto.get("actualSalesPct")));
            if (dto.containsKey("actualStPct"))
                gen.setActualStPct(toBigDecimal(dto.get("actualStPct")));
            gen.setUpdatedBy(userId);
            gen.setUpdatedAt(Instant.now());
            return planningGenderRepository.save(gen);
        }

        // Try to find in categories
        Optional<PlanningCategory> catOpt = planningCategoryRepository.findById(detailId)
            .filter(c -> c.getPlanningHeaderId().equals(planningId));
        if (catOpt.isPresent()) {
            PlanningCategory cat = catOpt.get();
            if (dto.containsKey("proposedBuyPct"))
                cat.setProposedBuyPct(toBigDecimal(dto.get("proposedBuyPct")));
            if (dto.containsKey("otbProposedAmount"))
                cat.setOtbProposedAmount(toBigDecimal(dto.get("otbProposedAmount")));
            if (dto.containsKey("actualBuyPct"))
                cat.setActualBuyPct(toBigDecimal(dto.get("actualBuyPct")));
            if (dto.containsKey("actualSalesPct"))
                cat.setActualSalesPct(toBigDecimal(dto.get("actualSalesPct")));
            if (dto.containsKey("actualStPct"))
                cat.setActualStPct(toBigDecimal(dto.get("actualStPct")));
            if (dto.containsKey("varLastyearPct"))
                cat.setVarLastyearPct(toBigDecimal(dto.get("varLastyearPct")));
            if (dto.containsKey("otbActualAmount"))
                cat.setOtbActualAmount(toBigDecimal(dto.get("otbActualAmount")));
            if (dto.containsKey("otbActualBuyPct"))
                cat.setOtbActualBuyPct(toBigDecimal(dto.get("otbActualBuyPct")));
            cat.setUpdatedBy(userId);
            cat.setUpdatedAt(Instant.now());
            return planningCategoryRepository.save(cat);
        }

        throw new DetailNotFoundException(detailId.toString());
    }

    // ─── CATEGORY FILTER OPTIONS ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PlanningFilterOptionsResponse getCategoryFilterOptions(String genderId, String categoryId) {
        List<Gender> genders = genderRepository.findAll(
            (root, cq, cb) -> cb.equal(root.get("isActive"), true));
        List<Category> categories;
        List<SubCategory> subCategories;

        if (genderId != null && !genderId.isBlank()) {
            categories = categoryRepository.findAll(
                (root, cq, cb) -> cb.and(
                    cb.equal(root.get("isActive"), true),
                    cb.equal(root.get("genderId"), Long.parseLong(genderId))));
        } else {
            categories = categoryRepository.findAll(
                (root, cq, cb) -> cb.equal(root.get("isActive"), true));
        }

        if (categoryId != null && !categoryId.isBlank()) {
            subCategories = subCategoryRepository.findAll(
                (root, cq, cb) -> cb.and(
                    cb.equal(root.get("isActive"), true),
                    cb.equal(root.get("categoryId"), Long.parseLong(categoryId))));
        } else if (genderId != null && !genderId.isBlank()) {
            List<Long> catIds = categories.stream().map(Category::getId).toList();
            if (catIds.isEmpty()) {
                subCategories = Collections.emptyList();
            } else {
                subCategories = subCategoryRepository.findAll(
                    (root, cq, cb) -> cb.and(
                        cb.equal(root.get("isActive"), true),
                        cb.in(root.get("categoryId")).value(catIds)));
            }
        } else {
            subCategories = subCategoryRepository.findAll(
                (root, cq, cb) -> cb.equal(root.get("isActive"), true));
        }

        return PlanningFilterOptionsResponse.builder()
            .genders(genders.stream()
                .map(g -> PlanningFilterOptionsResponse.GenderOption.builder()
                    .id(g.getId()).name(g.getName()).build())
                .toList())
            .categories(categories.stream()
                .map(c -> PlanningFilterOptionsResponse.CategoryOption.builder()
                    .id(c.getId()).name(c.getName()).genderId(c.getGenderId()).build())
                .toList())
            .subCategories(subCategories.stream()
                .map(s -> PlanningFilterOptionsResponse.SubCategoryOption.builder()
                    .id(s.getId()).name(s.getName()).categoryId(s.getCategoryId()).build())
                .toList())
            .build();
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    public void remove(Long id) {
        PlanningHeader header = planningHeaderRepository.findById(id)
            .orElseThrow(() -> new PlanningNotFoundException(id.toString()));

        if (!"DRAFT".equals(header.getStatus())) {
            throw new IllegalStateException("Only draft plannings can be deleted. Current status: " + header.getStatus());
        }

        planningCollectionRepository.deleteByPlanningHeaderId(id);
        planningGenderRepository.deleteByPlanningHeaderId(id);
        planningCategoryRepository.deleteByPlanningHeaderId(id);
        planningHeaderRepository.delete(header);
    }

    // ─── MAPPING HELPERS ───────────────────────────────────────────────────────

    private PlanningResponse toResponse(PlanningHeader header,
            List<PlanningCollection> collections,
            List<PlanningGender> genders,
            List<PlanningCategory> categories) {

        // Resolve allocate header brand
        PlanningResponse.BrandSummary brandSummary = null;
        if (header.getAllocateHeader() != null && header.getAllocateHeader().getBrand() != null) {
            Brand b = header.getAllocateHeader().getBrand();
            brandSummary = PlanningResponse.BrandSummary.builder()
                .id(b.getId())
                .code(b.getCode())
                .name(b.getName())
                .groupBrandName(b.getGroupBrand() != null ? b.getGroupBrand().getName() : null)
                .build();
        }

        PlanningResponse.AllocateHeaderSummary ahSummary = null;
        if (header.getAllocateHeader() != null) {
            AllocateHeader ah = header.getAllocateHeader();
            ahSummary = PlanningResponse.AllocateHeaderSummary.builder()
                .id(ah.getId())
                .version(ah.getVersion())
                .isFinalVersion(ah.getIsFinalVersion())
                .brand(brandSummary)
                .build();
        }

        // Resolve creator
        PlanningResponse.CreatorSummary creatorSummary = null;
        if (header.getCreator() != null) {
            User c = header.getCreator();
            creatorSummary = PlanningResponse.CreatorSummary.builder()
                .id(c.getId())
                .name(c.getName())
                .email(c.getEmail())
                .build();
        }

        // Collections with resolved names
        List<PlanningResponse.CollectionSummary> collectionSummaries = new ArrayList<>();
        if (collections != null) {
            Map<Long, SeasonType> seasonTypeMap = seasonTypeRepository.findAll().stream()
                .collect(Collectors.toMap(SeasonType::getId, st -> st));
            Map<Long, Store> storeMap = storeRepository.findAll().stream()
                .collect(Collectors.toMap(Store::getId, s -> s));

            for (PlanningCollection c : collections) {
                SeasonType st = seasonTypeMap.get(c.getSeasonTypeId());
                Store store = storeMap.get(c.getStoreId());
                collectionSummaries.add(PlanningResponse.CollectionSummary.builder()
                    .id(c.getId())
                    .seasonTypeId(c.getSeasonTypeId())
                    .seasonTypeName(st != null ? st.getName() : null)
                    .storeId(c.getStoreId())
                    .storeName(store != null ? store.getName() : null)
                    .actualBuyPct(c.getActualBuyPct())
                    .actualSalesPct(c.getActualSalesPct())
                    .actualStPct(c.getActualStPct())
                    .actualMoc(c.getActualMoc())
                    .proposedBuyPct(c.getProposedBuyPct())
                    .otbProposedAmount(c.getOtbProposedAmount())
                    .pctVarVsLast(c.getPctVarVsLast())
                    .build());
            }
        }

        // Genders with resolved names
        List<PlanningResponse.GenderSummary> genderSummaries = new ArrayList<>();
        if (genders != null) {
            Map<Long, Gender> genderMap = genderRepository.findAll().stream()
                .collect(Collectors.toMap(Gender::getId, g -> g));
            Map<Long, Store> storeMap = storeRepository.findAll().stream()
                .collect(Collectors.toMap(Store::getId, s -> s));

            for (PlanningGender g : genders) {
                Gender gender = genderMap.get(g.getGenderId());
                Store store = storeMap.get(g.getStoreId());
                genderSummaries.add(PlanningResponse.GenderSummary.builder()
                    .id(g.getId())
                    .genderId(g.getGenderId())
                    .genderName(gender != null ? gender.getName() : null)
                    .storeId(g.getStoreId())
                    .storeName(store != null ? store.getName() : null)
                    .actualBuyPct(g.getActualBuyPct())
                    .actualSalesPct(g.getActualSalesPct())
                    .actualStPct(g.getActualStPct())
                    .proposedBuyPct(g.getProposedBuyPct())
                    .otbProposedAmount(g.getOtbProposedAmount())
                    .pctVarVsLast(g.getPctVarVsLast())
                    .build());
            }
        }

        // Categories with resolved names (subcategory → category → gender)
        List<PlanningResponse.CategorySummary> categorySummaries = new ArrayList<>();
        if (categories != null) {
            Map<Long, SubCategory> subCatMap = subCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(SubCategory::getId, sc -> sc));
            Map<Long, Category> catMap = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
            Map<Long, Gender> genderMap = genderRepository.findAll().stream()
                .collect(Collectors.toMap(Gender::getId, g -> g));

            for (PlanningCategory c : categories) {
                SubCategory sc = subCatMap.get(c.getSubcategoryId());
                Category cat = sc != null ? catMap.get(sc.getCategoryId()) : null;
                Gender gender = cat != null ? genderMap.get(cat.getGenderId()) : null;
                categorySummaries.add(PlanningResponse.CategorySummary.builder()
                    .id(c.getId())
                    .subcategoryId(c.getSubcategoryId())
                    .subcategoryName(sc != null ? sc.getName() : null)
                    .categoryId(sc != null ? sc.getCategoryId() : null)
                    .categoryName(cat != null ? cat.getName() : null)
                    .genderId(cat != null ? cat.getGenderId() : null)
                    .genderName(gender != null ? gender.getName() : null)
                    .actualBuyPct(c.getActualBuyPct())
                    .actualSalesPct(c.getActualSalesPct())
                    .actualStPct(c.getActualStPct())
                    .proposedBuyPct(c.getProposedBuyPct())
                    .otbProposedAmount(c.getOtbProposedAmount())
                    .varLastyearPct(c.getVarLastyearPct())
                    .otbActualAmount(c.getOtbActualAmount())
                    .otbActualBuyPct(c.getOtbActualBuyPct())
                    .build());
            }
        }

        return PlanningResponse.builder()
            .id(header.getId())
            .version(header.getVersion())
            .status(header.getStatus())
            .isFinalVersion(header.getIsFinalVersion())
            .createdAt(header.getCreatedAt())
            .updatedAt(header.getUpdatedAt())
            .creator(creatorSummary)
            .allocateHeader(ahSummary)
            .collections(collectionSummaries)
            .genders(genderSummaries)
            .categories(categorySummaries)
            .build();
    }

    private PlanningResponse.HeaderSummary toHeaderSummary(PlanningHeader header) {
        PlanningResponse.CreatorSummary creatorSummary = null;
        if (header.getCreator() != null) {
            creatorSummary = PlanningResponse.CreatorSummary.builder()
                .id(header.getCreator().getId())
                .name(header.getCreator().getName())
                .email(header.getCreator().getEmail())
                .build();
        }

        PlanningResponse.BrandSummary brandSummary = null;
        PlanningResponse.AllocateHeaderSummary ahSummary = null;
        if (header.getAllocateHeader() != null) {
            AllocateHeader ah = header.getAllocateHeader();
            if (ah.getBrand() != null) {
                Brand b = ah.getBrand();
                brandSummary = PlanningResponse.BrandSummary.builder()
                    .id(b.getId())
                    .code(b.getCode())
                    .name(b.getName())
                    .groupBrandName(b.getGroupBrand() != null ? b.getGroupBrand().getName() : null)
                    .build();
            }
            ahSummary = PlanningResponse.AllocateHeaderSummary.builder()
                .id(ah.getId())
                .version(ah.getVersion())
                .isFinalVersion(ah.getIsFinalVersion())
                .brand(brandSummary)
                .build();
        }

        int collectionCount = planningCollectionRepository.findByPlanningHeaderId(header.getId()).size();
        int genderCount = planningGenderRepository.findByPlanningHeaderId(header.getId()).size();
        int categoryCount = planningCategoryRepository.findByPlanningHeaderId(header.getId()).size();

        return PlanningResponse.HeaderSummary.builder()
            .id(header.getId())
            .version(header.getVersion())
            .status(header.getStatus())
            .isFinalVersion(header.getIsFinalVersion())
            .createdAt(header.getCreatedAt())
            .creator(creatorSummary)
            .allocateHeader(ahSummary)
            .collectionCount(collectionCount)
            .genderCount(genderCount)
            .categoryCount(categoryCount)
            .build();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number num) return BigDecimal.valueOf(num.doubleValue());
        return new BigDecimal(value.toString());
    }

    // ─── EXCEPTION CLASSES ─────────────────────────────────────────────────────

    public static class PlanningNotFoundException extends RuntimeException {
        public PlanningNotFoundException(String id) {
            super("Planning header not found: " + id);
        }
    }

    public static class DetailNotFoundException extends RuntimeException {
        public DetailNotFoundException(String id) {
            super("Planning detail not found: " + id);
        }
    }
}
