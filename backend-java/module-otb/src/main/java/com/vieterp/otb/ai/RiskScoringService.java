package com.vieterp.otb.ai;

import com.vieterp.otb.domain.*;
import com.vieterp.otb.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class RiskScoringService {

    private final SKUProposalHeaderRepository skuProposalHeaderRepository;
    private final ProposalSizingHeaderRepository proposalSizingHeaderRepository;
    private final SKUProposalRepository skuProposalRepository;
    private final ProposalSizingRepository proposalSizingRepository;
    private final AllocateHeaderRepository allocateHeaderRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ProductRepository productRepository;

    public RiskAssessmentResult assessProposal(Long headerId) {
        SKUProposalHeader header = skuProposalHeaderRepository.findById(headerId)
            .orElseThrow(() -> new IllegalArgumentException("Proposal header not found: " + headerId));

        // Get proposals under this header
        List<SKUProposal> proposals = skuProposalRepository.findBySkuProposalHeaderId(headerId);

        // Get sizing data via header lookup
        List<ProposalSizingHeader> sizingHeaders = proposalSizingHeaderRepository.findBySkuProposalHeaderId(headerId);
        List<ProposalSizing> sizings = new ArrayList<>();
        for (ProposalSizingHeader sh : sizingHeaders) {
            sizings.addAll(proposalSizingRepository.findByProposalSizingHeaderId(sh.getId()));
        }

        // Get allocate header for budget info
        AllocateHeader allocateHeader = header.getAllocateHeader();
        Budget budget = allocateHeader != null ? allocateHeader.getBudget() : null;
        BigDecimal budgetAmount = budget != null ? budget.getAmount() : BigDecimal.ZERO;

        // Calculate component scores
        ComponentScore skuDiversity = assessSkuDiversity(proposals);
        ComponentScore storeAllocation = assessStoreAllocation(proposals, budgetAmount);
        ComponentScore sizingCoverage = assessSizingCoverage(sizings);
        ComponentScore marginImpact = assessMarginImpact(proposals);
        ComponentScore categoryBalance = assessCategoryBalance(proposals);

        // Weighted overall score
        double overallScore =
            skuDiversity.score() * 0.25 +
            storeAllocation.score() * 0.20 +
            sizingCoverage.score() * 0.20 +
            marginImpact.score() * 0.20 +
            categoryBalance.score() * 0.15;

        String riskLevel = overallScore >= 80 ? "low" :
                          overallScore >= 60 ? "medium" : "high";

        return new RiskAssessmentResult(
            headerId,
            overallScore,
            riskLevel,
            skuDiversity,
            storeAllocation,
            sizingCoverage,
            marginImpact,
            categoryBalance
        );
    }

    private ComponentScore assessSkuDiversity(List<SKUProposal> proposals) {
        if (proposals.isEmpty()) {
            return new ComponentScore(0.0, "No SKUs", "critical", "Add SKU proposals to reduce risk");
        }

        // Count unique products
        Set<Long> uniqueProducts = new HashSet<>();
        for (SKUProposal p : proposals) {
            if (p.getProductId() != null) {
                uniqueProducts.add(p.getProductId());
            }
        }

        int uniqueCount = uniqueProducts.size();
        int totalCount = proposals.size();

        // Good diversity if many SKUs or all unique
        double score = totalCount <= 5 ? 50.0 :
                       uniqueCount >= totalCount * 0.8 ? 90.0 :
                       uniqueCount >= totalCount * 0.5 ? 70.0 : 40.0;

        String status = score >= 80 ? "good" : score >= 50 ? "warning" : "critical";
        String suggestion = uniqueCount < 10 ? "Consider adding more SKU diversity" : "Good SKU diversity";

        return new ComponentScore(score, String.format("%d unique products", uniqueCount), status, suggestion);
    }

    private ComponentScore assessStoreAllocation(List<SKUProposal> proposals, BigDecimal budgetAmount) {
        // Store allocation is tracked at the BudgetAllocate level, not SKUProposal level.
        // Return a placeholder indicating this assessment lives in the budget allocation module.
        return new ComponentScore(50.0, "Store allocation managed at budget level", "info",
            "Store distribution is tracked via BudgetAllocate entities");
    }

    private ComponentScore assessSizingCoverage(List<ProposalSizing> sizings) {
        if (sizings.isEmpty()) {
            return new ComponentScore(0.0, "No sizing data", "critical", "Add size proposals");
        }

        // Check if all sizes have quantities
        long sizesWithQuantity = sizings.stream()
            .filter(s -> s.getProposalQuantity() != null && s.getProposalQuantity() > 0)
            .count();

        double coverage = (double) sizesWithQuantity / sizings.size();

        double score = coverage >= 0.9 ? 90.0 :
                       coverage >= 0.7 ? 75.0 :
                       coverage >= 0.5 ? 55.0 : 30.0;

        String status = score >= 80 ? "good" : score >= 50 ? "warning" : "critical";
        String suggestion = coverage < 0.9 ? "Some sizes missing quantities" : "Good size coverage";

        return new ComponentScore(score, String.format("%.0f%% coverage", coverage * 100), status, suggestion);
    }

    private ComponentScore assessMarginImpact(List<SKUProposal> proposals) {
        if (proposals.isEmpty()) {
            return new ComponentScore(0.0, "No SKUs", "critical", "Add SKU proposals");
        }

        // Check SRP vs Unit Cost ratio
        List<Double> margins = new ArrayList<>();
        for (SKUProposal p : proposals) {
            if (p.getSrp() != null && p.getUnitCost() != null &&
                p.getSrp().compareTo(BigDecimal.ZERO) > 0) {
                double margin = (p.getSrp().subtract(p.getUnitCost()))
                    .divide(p.getSrp(), 4, java.math.RoundingMode.HALF_UP)
                    .doubleValue();
                margins.add(margin);
            }
        }

        if (margins.isEmpty()) {
            return new ComponentScore(50.0, "Unknown margins", "warning", "Add pricing data");
        }

        double avgMargin = margins.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double score = avgMargin >= 0.40 ? 90.0 :
                       avgMargin >= 0.30 ? 75.0 :
                       avgMargin >= 0.20 ? 55.0 : 25.0;

        String status = score >= 80 ? "good" : score >= 50 ? "warning" : "critical";
        String suggestion = avgMargin < 0.30 ? "Consider higher margin products" : "Good margin profile";

        return new ComponentScore(score, String.format("%.0f%% avg margin", avgMargin * 100), status, suggestion);
    }

    private ComponentScore assessCategoryBalance(List<SKUProposal> proposals) {
        if (proposals.isEmpty()) {
            return new ComponentScore(0.0, "No SKUs", "critical", "Add SKU proposals");
        }

        // Group by subCategoryId
        Map<Long, Long> byCategory = new HashMap<>();
        for (SKUProposal p : proposals) {
            if (p.getProductId() != null) {
                Product product = productRepository.findById(p.getProductId()).orElse(null);
                if (product != null && product.getSubCategoryId() != null) {
                    byCategory.merge(product.getSubCategoryId(), 1L, Long::sum);
                }
            }
        }

        if (byCategory.isEmpty()) {
            return new ComponentScore(50.0, "Category unknown", "warning", "Link products to categories");
        }

        int categoryCount = byCategory.size();
        long total = proposals.size();

        // Check balance - ideal is multiple categories with reasonable distribution
        double score = categoryCount >= 5 ? 90.0 :
                       categoryCount >= 3 ? 75.0 :
                       categoryCount >= 2 ? 55.0 : 30.0;

        String status = score >= 80 ? "good" : score >= 50 ? "warning" : "critical";
        String suggestion = categoryCount < 3 ? "Consider adding categories" : "Category balance looks good";

        return new ComponentScore(score, String.format("%d categories", categoryCount), status, suggestion);
    }

    public record RiskAssessmentResult(
        Long headerId,
        Double overallScore,
        String riskLevel,
        ComponentScore skuDiversity,
        ComponentScore storeAllocation,
        ComponentScore sizingCoverage,
        ComponentScore marginImpact,
        ComponentScore categoryBalance
    ) {}

    public record ComponentScore(
        Double score,
        String detail,
        String status,
        String suggestion
    ) {}
}
