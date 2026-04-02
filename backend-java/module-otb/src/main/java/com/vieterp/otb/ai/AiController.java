package com.vieterp.otb.ai;

import com.vieterp.otb.ai.dto.*;
import com.vieterp.otb.ai.RiskScoringService.RiskAssessmentResult;
import com.vieterp.otb.ai.SkuRecommenderService.RecommendationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "AI-based features for OTB optimization")
public class AiController {

    private final AiService aiService;
    private final BudgetAlertsService budgetAlertsService;
    private final OtbAllocationService otbAllocationService;
    private final RiskScoringService riskScoringService;
    private final SkuRecommenderService skuRecommenderService;

    // ─── SIZE CURVE ───────────────────────────────────────────────────────────

    @PostMapping("/size-curve/calculate")
    @Operation(summary = "Calculate recommended size curve distribution")
    public ResponseEntity<List<SizeCurveResponse>> calculateSizeCurve(
            @RequestBody CalculateSizeCurveDto dto) {
        List<SizeCurveResponse> result = aiService.calculateSizeCurve(
            dto.getSubCategoryId(),
            dto.getStoreId(),
            dto.getTotalOrderQty()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/size-curve/compare")
    @Operation(summary = "Compare user's size distribution against recommended curve")
    public ResponseEntity<CompareSizeCurveResponse> compareSizeCurve(
            @RequestBody CompareSizeCurveDto dto) {
        CompareSizeCurveResponse result = aiService.compareSizeCurve(
            dto.getSubCategoryId(),
            dto.getStoreId(),
            dto.getUserSizing()
        );
        return ResponseEntity.ok(result);
    }

    // ─── BUDGET ALERTS ─────────────────────────────────────────────────────────

    @GetMapping("/alerts")
    @Operation(summary = "Get budget alerts with optional filters")
    public ResponseEntity<List<AlertDto>> getAlerts(
            @Parameter(description = "Budget ID filter") @RequestParam(name = "budgetId", required = false) Long budgetId,
            @Parameter(description = "Only unread alerts") @RequestParam(name = "unreadOnly", required = false) Boolean unreadOnly) {
        GetAlertsQueryDto options = new GetAlertsQueryDto();
        options.setBudgetId(budgetId);
        options.setUnreadOnly(unreadOnly);
        return ResponseEntity.ok(budgetAlertsService.getAlerts(options));
    }

    @PostMapping("/alerts/check")
    @Operation(summary = "Check all approved budgets and generate alerts")
    public ResponseEntity<List<AlertDto>> checkAllBudgets() {
        return ResponseEntity.ok(budgetAlertsService.checkAllBudgets());
    }

    @PatchMapping("/alerts/{id}/read")
    @Operation(summary = "Mark alert as read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(budgetAlertsService.markAsRead(id));
    }

    @PatchMapping("/alerts/{id}/dismiss")
    @Operation(summary = "Dismiss alert")
    public ResponseEntity<Map<String, Object>> dismissAlert(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(budgetAlertsService.dismissAlert(id));
    }

    // ─── ALLOCATION ───────────────────────────────────────────────────────────

    @PostMapping("/allocation/generate")
    @Operation(summary = "Generate OTB allocation recommendations")
    public ResponseEntity<AllocationResultDto> generateAllocation(
            @RequestBody GenerateAllocationDto dto) {
        return ResponseEntity.ok(otbAllocationService.generateAllocation(dto));
    }

    @PostMapping("/allocation/compare")
    @Operation(summary = "Compare user allocation against recommended distribution")
    public ResponseEntity<Map<String, Object>> compareAllocation(
            @RequestBody CompareAllocationDto dto) {
        return ResponseEntity.ok(otbAllocationService.compareAllocation(dto));
    }

    // ─── RISK ASSESSMENT ──────────────────────────────────────────────────────

    @PostMapping("/risk/assess/{headerId}")
    @Operation(summary = "Assess proposal risk scores")
    public ResponseEntity<RiskAssessmentResult> assessRisk(
            @PathVariable(name = "headerId") Long headerId) {
        return ResponseEntity.ok(riskScoringService.assessProposal(headerId));
    }

    // ─── SKU RECOMMENDATIONS ─────────────────────────────────────────────────

    @PostMapping("/sku-recommend/generate")
    @Operation(summary = "Generate SKU recommendations based on criteria")
    public ResponseEntity<RecommendationResult> generateSkuRecommendations(
            @RequestBody GenerateSkuRecommendationsDto dto) {
        return ResponseEntity.ok(skuRecommenderService.generateRecommendations(dto));
    }

    @PostMapping("/sku-recommend/add-to-proposal")
    @Operation(summary = "Add recommended SKUs to a proposal header")
    public ResponseEntity<Map<String, Object>> addToProposal(
            @RequestBody AddRecommendationsToProposalDto dto) {
        Integer added = skuRecommenderService.addSelectedToProposal(dto.getProductIds(), dto.getHeaderId());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "addedCount", added,
            "message", added + " SKUs added to proposal"
        ));
    }
}
