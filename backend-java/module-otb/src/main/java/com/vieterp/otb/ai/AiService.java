package com.vieterp.otb.ai;

import com.vieterp.otb.ai.dto.*;
import com.vieterp.otb.domain.SubcategorySize;
import com.vieterp.otb.domain.repository.SubcategorySizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AiService {

    private final SubcategorySizeRepository subcategorySizeRepository;

    // Bell curve distributions by number of sizes
    private static final Map<Integer, double[]> SIZE_CURVES = Map.of(
        3, new double[]{0.25, 0.50, 0.25},
        4, new double[]{0.15, 0.35, 0.35, 0.15},
        5, new double[]{0.10, 0.20, 0.40, 0.20, 0.10},
        6, new double[]{0.05, 0.15, 0.30, 0.30, 0.15, 0.05}
    );

    private static final double CONFIDENCE_KNOWN = 0.7;
    private static final double CONFIDENCE_DEFAULT = 0.3;

    public List<SizeCurveResponse> calculateSizeCurve(Long subCategoryId, Long storeId, Integer totalOrderQty) {
        List<SubcategorySize> sizes = subcategorySizeRepository.findBySubCategoryId(subCategoryId);

        if (sizes.isEmpty()) {
            return Collections.emptyList();
        }

        int sizeCount = sizes.size();
        double[] curve = SIZE_CURVES.getOrDefault(sizeCount, generateDefaultCurve(sizeCount));
        double confidence = sizes.stream().allMatch(s -> s.getName() != null) ? CONFIDENCE_KNOWN : CONFIDENCE_DEFAULT;

        List<SizeCurveResponse> responses = new ArrayList<>();
        for (int i = 0; i < sizes.size(); i++) {
            SubcategorySize size = sizes.get(i);
            double pct = curve[i];
            int qty = (int) Math.round(totalOrderQty * pct);
            String reasoning = buildReasoning(sizeCount, i, pct);

            responses.add(new SizeCurveResponse(
                size.getName(),
                pct,
                qty,
                confidence,
                reasoning
            ));
        }

        return responses;
    }

    public CompareSizeCurveResponse compareSizeCurve(Long subCategoryId, Long storeId, Map<String, Integer> userSizing) {
        if (userSizing == null || userSizing.isEmpty()) {
            return new CompareSizeCurveResponse(
                "risk",
                0,
                Collections.emptyList(),
                "No user sizing data provided"
            );
        }

        int totalUserQty = userSizing.values().stream().mapToInt(Integer::intValue).sum();
        if (totalUserQty == 0) {
            return new CompareSizeCurveResponse(
                "risk",
                0,
                Collections.emptyList(),
                "User sizing totals to zero"
            );
        }

        // Get recommended curve
        List<SizeCurveResponse> recommended = calculateSizeCurve(subCategoryId, storeId, totalUserQty);

        // Build deviation list
        List<CompareSizeCurveResponse.SizeDeviation> deviations = new ArrayList<>();
        int totalScore = 0;

        for (SizeCurveResponse rec : recommended) {
            Integer userQty = userSizing.get(rec.sizeName());
            double userPct = userQty != null ? (double) userQty / totalUserQty : 0.0;
            double deviation = userPct - rec.recommendedPct();
            String severity = Math.abs(deviation) < 0.05 ? "low" :
                             Math.abs(deviation) < 0.10 ? "medium" : "high";

            deviations.add(new CompareSizeCurveResponse.SizeDeviation(
                rec.sizeName(),
                rec.recommendedPct(),
                userPct,
                deviation,
                severity
            ));

            // Score contribution (max 100 points total, normalized by size count)
            int sizeScore = Math.max(0, 100 - (int) (Math.abs(deviation) * 500));
            totalScore += sizeScore;
        }

        int avgScore = recommended.isEmpty() ? 0 : totalScore / recommended.size();
        String alignment = avgScore > 80 ? "good" : avgScore >= 50 ? "warning" : "risk";
        String suggestion = buildSuggestion(alignment, deviations);

        return new CompareSizeCurveResponse(alignment, avgScore, deviations, suggestion);
    }

    private double[] generateDefaultCurve(int sizeCount) {
        // Generate a symmetric bell curve for any size count
        double[] curve = new double[sizeCount];
        double center = (sizeCount - 1) / 2.0;
        double sigma = sizeCount / 4.0;
        double sum = 0.0;

        for (int i = 0; i < sizeCount; i++) {
            double x = i - center;
            curve[i] = Math.exp(-(x * x) / (2 * sigma * sigma));
            sum += curve[i];
        }

        // Normalize
        for (int i = 0; i < sizeCount; i++) {
            curve[i] /= sum;
        }
        return curve;
    }

    private String buildReasoning(int sizeCount, int index, double pct) {
        return String.format("Size %d of %d with %.0f%% distribution based on standard bell curve model",
            index + 1, sizeCount, pct * 100);
    }

    private String buildSuggestion(String alignment, List<CompareSizeCurveResponse.SizeDeviation> deviations) {
        if ("good".equals(alignment)) {
            return "Your size distribution aligns well with the recommended curve.";
        }

        List<String> issues = deviations.stream()
            .filter(d -> Math.abs(d.deviation()) > 0.05)
            .map(d -> String.format("%s: expected %.0f%% but got %.0f%%",
                d.sizeName(), d.expectedPct() * 100, d.actualPct() * 100))
            .toList();

        if ("warning".equals(alignment)) {
            return "Consider adjusting: " + String.join(", ", issues);
        }
        return "Significant deviations detected. Review: " + String.join(", ", issues);
    }
}
