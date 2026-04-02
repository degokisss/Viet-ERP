package com.vieterp.otb.ai;

import com.vieterp.otb.ai.dto.AlertDto;
import com.vieterp.otb.ai.dto.GetAlertsQueryDto;
import com.vieterp.otb.domain.AllocateHeader;
import com.vieterp.otb.domain.Budget;
import com.vieterp.otb.domain.BudgetAllocate;
import com.vieterp.otb.domain.repository.AllocateHeaderRepository;
import com.vieterp.otb.domain.repository.BudgetAllocateRepository;
import com.vieterp.otb.budget.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class BudgetAlertsService {

    private final BudgetRepository budgetRepository;
    private final AllocateHeaderRepository allocateHeaderRepository;
    private final BudgetAllocateRepository budgetAllocateRepository;

    private final Map<Long, AlertDto> alertStore = new HashMap<>();
    private Long alertIdCounter = 1L;

    @Transactional(readOnly = true)
    public List<AlertDto> getAlerts(GetAlertsQueryDto options) {
        List<AlertDto> results = new ArrayList<>(alertStore.values());

        if (options != null) {
            if (options.getBudgetId() != null) {
                results = results.stream()
                    .filter(a -> a.getBudgetId().equals(options.getBudgetId()))
                    .toList();
            }
            if (Boolean.TRUE.equals(options.getUnreadOnly())) {
                results = results.stream()
                    .filter(a -> !Boolean.TRUE.equals(a.getIsRead()))
                    .toList();
            }
        }

        return results;
    }

    public List<AlertDto> checkAllBudgets() {
        List<AlertDto> newAlerts = new ArrayList<>();

        Specification<Budget> spec = (root, cq, cb) -> cb.in(root.get("status")).value(List.of("APPROVED", "SUBMITTED"));
        List<Budget> budgets = budgetRepository.findAll(spec);

        for (Budget budget : budgets) {
            List<AllocateHeader> headers = allocateHeaderRepository.findAll((root, cq, cb) ->
                cb.equal(root.get("budget"), budget));

            for (AllocateHeader header : headers) {
                List<BudgetAllocate> allocates = budgetAllocateRepository.findAll((root, cq, cb) ->
                    cb.equal(root.get("allocateHeader"), header));

                newAlerts.addAll(analyzeAllocations(budget, header, allocates));
            }
        }

        // Store alerts
        for (AlertDto alert : newAlerts) {
            AlertDto stored = AlertDto.builder()
                .id(alertIdCounter++)
                .budgetId(alert.getBudgetId())
                .budgetName(alert.getBudgetName())
                .alertType(alert.getAlertType())
                .severity(alert.getSeverity())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .isRead(false)
                .isDismissed(false)
                .build();
            alertStore.put(stored.getId(), stored);
        }

        return newAlerts;
    }

    public Map<String, Object> markAsRead(Long id) {
        AlertDto alert = alertStore.get(id);
        if (alert != null) {
            alert.setIsRead(true);
        }
        return Map.of("id", id, "isRead", alert != null && Boolean.TRUE.equals(alert.getIsRead()));
    }

    public Map<String, Object> dismissAlert(Long id) {
        AlertDto alert = alertStore.get(id);
        if (alert != null) {
            alert.setIsDismissed(true);
        }
        return Map.of("id", id, "isDismissed", alert != null && Boolean.TRUE.equals(alert.getIsDismissed()));
    }

    private List<AlertDto> analyzeAllocations(Budget budget, AllocateHeader header, List<BudgetAllocate> allocates) {
        List<AlertDto> alerts = new ArrayList<>();

        if (budget.getAmount() == null || allocates.isEmpty()) {
            return alerts;
        }

        BigDecimal totalAllocated = allocates.stream()
            .map(BudgetAllocate::getBudgetAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal budgetAmount = budget.getAmount();
        double utilization = budgetAmount.compareTo(BigDecimal.ZERO) > 0
            ? totalAllocated.doubleValue() / budgetAmount.doubleValue()
            : 0.0;

        // Over budget alert
        if (totalAllocated.compareTo(budgetAmount) > 0) {
            alerts.add(AlertDto.builder()
                .budgetId(budget.getId())
                .budgetName(budget.getName())
                .alertType("over_budget")
                .severity("critical")
                .title("Budget Exceeded")
                .message(String.format("Allocated amount %.2f exceeds budget %.2f by %.2f",
                    totalAllocated, budgetAmount, totalAllocated.subtract(budgetAmount)))
                .build());
        }

        // Approaching limit alert (>= 90%)
        if (utilization >= 0.90 && utilization < 1.0) {
            alerts.add(AlertDto.builder()
                .budgetId(budget.getId())
                .budgetName(budget.getName())
                .alertType("approaching_limit")
                .severity("warning")
                .title("Approaching Budget Limit")
                .message(String.format("Budget utilization at %.0f%%", utilization * 100))
                .build());
        }

        // Under utilized alert (< 50%)
        if (utilization < 0.50 && utilization > 0) {
            alerts.add(AlertDto.builder()
                .budgetId(budget.getId())
                .budgetName(budget.getName())
                .alertType("under_utilized")
                .severity("info")
                .title("Low Budget Utilization")
                .message(String.format("Only %.0f%% of budget allocated", utilization * 100))
                .build());
        }

        // Store concentration alert (> 60% in single store)
        for (BudgetAllocate alloc : allocates) {
            if (alloc.getBudgetAmount() != null && budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
                double storePct = alloc.getBudgetAmount().doubleValue() / budgetAmount.doubleValue();
                if (storePct > 0.60) {
                    String storeName = alloc.getStore() != null ? alloc.getStore().getName() : "Store " + alloc.getStore();
                    alerts.add(AlertDto.builder()
                        .budgetId(budget.getId())
                        .budgetName(budget.getName())
                        .alertType("store_concentration")
                        .severity("warning")
                        .title("High Store Concentration")
                        .message(String.format("%s has %.0f%% of budget allocation", storeName, storePct * 100))
                        .build());
                }
            }
        }

        return alerts;
    }
}
