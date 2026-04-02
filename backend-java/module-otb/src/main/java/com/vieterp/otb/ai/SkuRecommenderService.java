package com.vieterp.otb.ai;

import com.vieterp.otb.ai.dto.AddRecommendationsToProposalDto;
import com.vieterp.otb.ai.dto.GenerateSkuRecommendationsDto;
import com.vieterp.otb.domain.Product;
import com.vieterp.otb.domain.SKUProposal;
import com.vieterp.otb.domain.SKUProposalHeader;
import com.vieterp.otb.domain.repository.ProductRepository;
import com.vieterp.otb.domain.repository.SKUProposalHeaderRepository;
import com.vieterp.otb.domain.repository.SKUProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SkuRecommenderService {

    private final ProductRepository productRepository;
    private final SKUProposalHeaderRepository skuProposalHeaderRepository;
    private final SKUProposalRepository skuProposalRepository;

    public RecommendationResult generateRecommendations(GenerateSkuRecommendationsDto input) {
        List<Product> products;

        if (input.getSubCategoryId() != null && input.getBrandId() != null) {
            products = productRepository.findActiveBySubCategoryIdAndBrandId(
                input.getSubCategoryId(), input.getBrandId());
        } else if (input.getSubCategoryId() != null) {
            products = productRepository.findBySubCategoryId(input.getSubCategoryId());
        } else if (input.getBrandId() != null) {
            products = productRepository.findAll((root, cq, cb) ->
                cb.equal(root.get("brandId"), input.getBrandId()));
        } else {
            products = productRepository.findAll();
        }

        // Filter active products
        products = products.stream()
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
            .toList();

        if (products.isEmpty()) {
            return new RecommendationResult(
                Collections.emptyList(),
                0.0,
                List.of("No products found matching criteria")
            );
        }

        int maxResults = input.getMaxResults() != null ? input.getMaxResults() : 10;
        BigDecimal budgetAmount = input.getBudgetAmount() != null ? input.getBudgetAmount() : BigDecimal.ZERO;

        // Score products
        List<ScoredProduct> scored = products.stream()
            .map(p -> scoreProduct(p, budgetAmount))
            .sorted(Comparator.comparingDouble(ScoredProduct::score).reversed())
            .limit(maxResults)
            .toList();

        // Calculate overall confidence
        double avgScore = scored.stream()
            .mapToDouble(ScoredProduct::score)
            .average()
            .orElse(0.0);

        // Assign quantities proportional to score
        double totalScore = scored.stream().mapToDouble(ScoredProduct::score).sum();
        List<RecommendedSku> recommendations = new ArrayList<>();

        if (totalScore > 0) {
            for (ScoredProduct sp : scored) {
                double proportion = sp.score() / totalScore;
                int quantity = BigDecimal.valueOf(proportion * 100)
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
                quantity = Math.max(1, quantity); // Minimum 1

                recommendations.add(new RecommendedSku(
                    sp.product().getId(),
                    sp.product().getSkuCode(),
                    sp.product().getProductName(),
                    sp.score(),
                    proportion * 100,
                    quantity,
                    sp.reasons()
                ));
            }
        }

        return new RecommendationResult(recommendations, avgScore / 100.0, Collections.emptyList());
    }

    public Integer addSelectedToProposal(List<Long> productIds, Long headerId) {
        SKUProposalHeader header = skuProposalHeaderRepository.findById(headerId)
            .orElseThrow(() -> new IllegalArgumentException("Proposal header not found: " + headerId));

        List<Product> products = productRepository.findAllById(productIds);
        int added = 0;

        for (Product product : products) {
            // Check if already exists
            List<SKUProposal> existing = skuProposalRepository.findBySkuProposalHeaderId(headerId);
            boolean alreadyExists = existing.stream()
                .anyMatch(p -> p.getProductId() != null && p.getProductId().equals(product.getId()));

            if (!alreadyExists) {
                SKUProposal proposal = SKUProposal.builder()
                    .skuProposalHeaderId(header.getId())
                    .productId(product.getId())
                    .customerTarget(product.getProductName())
                    .unitCost(product.getSrp() != null ? product.getSrp().multiply(BigDecimal.valueOf(0.6)) : null)
                    .srp(product.getSrp())
                    .createdAt(Instant.now())
                    .build();
                skuProposalRepository.save(proposal);
                added++;
            }
        }

        return added;
    }

    private ScoredProduct scoreProduct(Product product, BigDecimal budgetAmount) {
        double score = 0.0;
        List<String> reasons = new ArrayList<>();

        // Data completeness (40% weight)
        int completeness = 0;
        if (product.getProductName() != null && !product.getProductName().isBlank()) completeness++;
        if (product.getSkuCode() != null && !product.getSkuCode().isBlank()) completeness++;
        if (product.getColor() != null && !product.getColor().isBlank()) completeness++;
        if (product.getComposition() != null && !product.getComposition().isBlank()) completeness++;
        if (product.getSrp() != null && product.getSrp().compareTo(BigDecimal.ZERO) > 0) completeness++;
        if (product.getImageUrl() != null && !product.getImageUrl().isBlank()) completeness++;

        double completenessScore = (completeness / 6.0) * 40;
        score += completenessScore;
        if (completeness >= 5) {
            reasons.add("Complete product data");
        }

        // Price fit (30% weight) - prefer products within budget range
        if (product.getSrp() != null && budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            double priceRatio = product.getSrp().doubleValue() / budgetAmount.doubleValue();
            if (priceRatio <= 0.1) {
                score += 30;
                reasons.add("Excellent price fit");
            } else if (priceRatio <= 0.3) {
                score += 20;
                reasons.add("Good price fit");
            } else if (priceRatio <= 0.5) {
                score += 10;
            }
        } else if (product.getSrp() != null) {
            score += 15; // No budget reference but has price
        }

        // Category coverage (30% weight) - prefer products in less covered categories
        // This would require looking at existing proposals, simplified here
        if (product.getSubCategoryId() != null) {
            score += 15;
        }
        if (product.getBrandId() != null) {
            score += 15;
        }

        return new ScoredProduct(product, Math.min(100, score), reasons);
    }

    private record ScoredProduct(Product product, double score, List<String> reasons) {}

    public record RecommendationResult(
        List<RecommendedSku> recommendations,
        Double overallConfidence,
        List<String> warnings
    ) {}

    public record RecommendedSku(
        Long productId,
        String skuCode,
        String productName,
        Double score,
        Double recommendedPercentage,
        Integer recommendedQuantity,
        List<String> reasons
    ) {}
}
