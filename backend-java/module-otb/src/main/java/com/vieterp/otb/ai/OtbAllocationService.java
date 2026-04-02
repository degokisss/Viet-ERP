package com.vieterp.otb.ai;

import com.vieterp.otb.ai.dto.*;
import com.vieterp.otb.domain.Category;
import com.vieterp.otb.domain.Gender;
import com.vieterp.otb.domain.SeasonType;
import com.vieterp.otb.domain.SubCategory;
import com.vieterp.otb.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class OtbAllocationService {

    private final SeasonTypeRepository seasonTypeRepository;
    private final GenderRepository genderRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    private static final double DEFAULT_CONFIDENCE = 0.75;

    public AllocationResultDto generateAllocation(GenerateAllocationDto input) {
        BigDecimal budgetAmount = input.getBudgetAmount();
        if (budgetAmount == null || budgetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new AllocationResultDto(
                BigDecimal.ZERO,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                0.0,
                List.of("Invalid budget amount")
            );
        }

        List<String> warnings = new ArrayList<>();

        // Recommend collections (equal distribution across active season types)
        List<SeasonType> activeSeasonTypes = seasonTypeRepository.findAll((root, cq, cb) ->
            cb.equal(root.get("isActive"), true));
        List<AllocationResultDto.CollectionRecommendation> collections = new ArrayList<>();

        if (activeSeasonTypes.isEmpty()) {
            warnings.add("No active season types found");
            activeSeasonTypes = List.of(SeasonType.builder().name("Default").isActive(true).build());
        }

        double equalPct = 1.0 / activeSeasonTypes.size();
        for (SeasonType st : activeSeasonTypes) {
            BigDecimal amount = budgetAmount.multiply(BigDecimal.valueOf(equalPct)).setScale(2, RoundingMode.HALF_UP);
            collections.add(new AllocationResultDto.CollectionRecommendation(
                st.getName(),
                amount,
                equalPct * 100,
                DEFAULT_CONFIDENCE
            ));
        }

        // Recommend genders (weighted by category count per gender)
        List<Gender> genders = genderRepository.findAll((root, cq, cb) ->
            cb.equal(root.get("isActive"), true));
        List<AllocationResultDto.GenderRecommendation> genderRecs = new ArrayList<>();

        Map<Long, Long> genderCategoryCount = new HashMap<>();
        for (Gender g : genders) {
            long count = categoryRepository.count((root, cq, cb) ->
                cb.equal(root.get("genderId"), g.getId()));
            genderCategoryCount.put(g.getId(), count);
        }

        long totalCategories = genderCategoryCount.values().stream().mapToLong(Long::longValue).sum();
        if (totalCategories == 0) {
            warnings.add("No categories found for gender weighting");
            totalCategories = genders.size();
            for (Gender g : genders) {
                genderCategoryCount.put(g.getId(), 1L);
            }
        }

        for (Gender g : genders) {
            long count = genderCategoryCount.getOrDefault(g.getId(), 0L);
            double weight = (double) count / totalCategories;
            BigDecimal amount = budgetAmount.multiply(BigDecimal.valueOf(weight)).setScale(2, RoundingMode.HALF_UP);
            genderRecs.add(new AllocationResultDto.GenderRecommendation(
                g.getName(),
                amount,
                weight * 100,
                DEFAULT_CONFIDENCE
            ));
        }

        // Recommend categories (weighted by subcategory count)
        List<Category> categories = categoryRepository.findAll((root, cq, cb) ->
            cb.equal(root.get("isActive"), true));
        List<AllocationResultDto.CategoryRecommendation> categoryRecs = new ArrayList<>();

        Map<Long, Long> categorySubCount = new HashMap<>();
        for (Category c : categories) {
            long count = subCategoryRepository.count((root, cq, cb) ->
                cb.equal(root.get("categoryId"), c.getId()));
            categorySubCount.put(c.getId(), count);
        }

        long totalSubcategories = categorySubCount.values().stream().mapToLong(Long::longValue).sum();
        if (totalSubcategories == 0) {
            warnings.add("No subcategories found for category weighting");
            totalSubcategories = categories.size();
            for (Category c : categories) {
                categorySubCount.put(c.getId(), 1L);
            }
        }

        for (Category c : categories) {
            long count = categorySubCount.getOrDefault(c.getId(), 0L);
            double weight = (double) count / totalSubcategories;
            BigDecimal amount = budgetAmount.multiply(BigDecimal.valueOf(weight)).setScale(2, RoundingMode.HALF_UP);
            categoryRecs.add(new AllocationResultDto.CategoryRecommendation(
                c.getName(),
                amount,
                weight * 100,
                DEFAULT_CONFIDENCE
            ));
        }

        return new AllocationResultDto(
            budgetAmount,
            collections,
            genderRecs,
            categoryRecs,
            DEFAULT_CONFIDENCE,
            warnings
        );
    }

    public Map<String, Object> compareAllocation(CompareAllocationDto input) {
        List<CompareAllocationDto.UserAllocationItem> userAllocation = input.getUserAllocation();
        BigDecimal budgetAmount = input.getBudgetAmount();
        BigDecimal totalUserAllocation = userAllocation.stream()
            .map(CompareAllocationDto.UserAllocationItem::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> warnings = new ArrayList<>();
        int score = 100;

        if (budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            double variance = Math.abs(totalUserAllocation.subtract(budgetAmount).doubleValue())
                / budgetAmount.doubleValue();
            if (variance > 0.1) {
                score -= (int) (variance * 100);
                warnings.add(String.format("Total allocation variance: %.1f%%", variance * 100));
            }
        }

        // Check for negative allocations
        boolean hasNegative = userAllocation.stream()
            .anyMatch(a -> a.getAmount() != null && a.getAmount().compareTo(BigDecimal.ZERO) < 0);
        if (hasNegative) {
            score -= 20;
            warnings.add("Negative allocation amounts detected");
        }

        // Check for missing categories
        long zeroAllocations = userAllocation.stream()
            .filter(a -> a.getAmount() == null || a.getAmount().compareTo(BigDecimal.ZERO) == 0)
            .count();
        if (zeroAllocations > userAllocation.size() / 2) {
            score -= 10;
            warnings.add("Many categories have zero allocation");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("score", Math.max(0, score));
        result.put("isBalanced", score >= 80);
        result.put("totalUserAllocation", totalUserAllocation);
        result.put("budgetAmount", budgetAmount);
        result.put("warnings", warnings);

        return result;
    }
}
