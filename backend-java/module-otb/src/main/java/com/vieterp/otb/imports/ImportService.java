package com.vieterp.otb.imports;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vieterp.otb.domain.*;
import com.vieterp.otb.domain.repository.*;
import com.vieterp.otb.imports.dto.*;
import com.vieterp.otb.imports.repository.ImportedRecordRepository;
import com.vieterp.otb.imports.repository.ImportSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ImportService {

    private final ImportSessionRepository importSessionRepository;
    private final ImportedRecordRepository importedRecordRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    // ─── APPLY RESULT ───────────────────────────────────────────────────────────

    public record ApplyResult(int success, int failed, List<String> errors) {}

    // ─── PROCESS BATCH ─────────────────────────────────────────────────────────

    public Map<String, Object> processBatch(ImportBatchDto dto, Long userId) {
        ImportSession session = getOrCreateSession(dto.getSessionId(), dto.getTarget(), userId);

        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < dto.getRows().size(); i++) {
            Map<String, Object> row = dto.getRows().get(i);
            int rowNumber = i + 1;

            try {
                String rowData = objectMapper.writeValueAsString(row);
                ImportedRecord record = ImportedRecord.builder()
                    .sessionId(session.getId())
                    .rowNumber(rowNumber)
                    .data(rowData)
                    .status("PENDING")
                    .createdAt(Instant.now())
                    .build();
                importedRecordRepository.save(record);
                successCount++;
            } catch (Exception e) {
                String rowData;
                try {
                    rowData = objectMapper.writeValueAsString(row);
                } catch (Exception ex) {
                    rowData = row.toString();
                }
                ImportedRecord record = ImportedRecord.builder()
                    .sessionId(session.getId())
                    .rowNumber(rowNumber)
                    .data(rowData)
                    .status("ERROR")
                    .error(e.getMessage())
                    .createdAt(Instant.now())
                    .build();
                importedRecordRepository.save(record);
                errorCount++;
                errors.add("Row " + rowNumber + ": " + e.getMessage());
            }
        }

        // Update session totals
        session.setTotalRows((session.getTotalRows() != null ? session.getTotalRows() : 0) + dto.getRows().size());
        session.setSuccessRows((session.getSuccessRows() != null ? session.getSuccessRows() : 0) + successCount);
        session.setErrorRows((session.getErrorRows() != null ? session.getErrorRows() : 0) + errorCount);
        session.setUpdatedAt(Instant.now());
        importSessionRepository.save(session);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("processed", dto.getRows().size());
        result.put("success", successCount);
        result.put("errors", errorCount);
        result.put("batchIndex", dto.getBatchIndex());
        result.put("totalBatches", dto.getTotalBatches());

        return result;
    }

    // ─── QUERY DATA ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> queryData(ImportQueryDto query) {
        Specification<ImportedRecord> spec = Specification.where(null);

        if (query.getTarget() != null) {
            spec = spec.and((root, cq, cb) -> {
                Subquery<Long> sessionSubquery = cq.subquery(Long.class);
                var sessionRoot = sessionSubquery.from(ImportSession.class);
                sessionSubquery.select(sessionRoot.get("id"))
                    .where(cb.equal(sessionRoot.get("target"), query.getTarget().name()));
                return cb.in(root.get("sessionId")).value(sessionSubquery);
            });
        }

        if (query.getSearch() != null && !query.getSearch().isBlank()) {
            spec = spec.and((root, cq, cb) ->
                cb.or(
                    cb.like(root.get("status"), "%" + query.getSearch() + "%"),
                    cb.like(root.get("data"), "%" + query.getSearch() + "%")
                )
            );
        }

        Sort.Direction direction = "ASC".equalsIgnoreCase(query.getSortOrder())
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(query.getPage() - 1, query.getPageSize(),
            Sort.by(direction, query.getSortBy()));

        Page<ImportedRecord> pageResult = importedRecordRepository.findAll(spec, pageable);

        List<Map<String, Object>> data = pageResult.getContent().stream()
            .map(this::recordToMap)
            .toList();

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", query.getPage());
        meta.put("pageSize", query.getPageSize());
        meta.put("total", pageResult.getTotalElements());
        meta.put("totalPages", pageResult.getTotalPages());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("meta", meta);
        return result;
    }

    // ─── GET STATS ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getStats(String target) {
        if (target == null || target.isBlank()) {
            return getAllTargetStats();
        }

        List<ImportSession> sessions = importSessionRepository.findAll((root, cq, cb) ->
            cb.equal(root.get("target"), target));

        return buildTargetStats(target, sessions);
    }

    // ─── GET ALL TARGET STATS ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getAllTargetStats() {
        Map<String, Object> result = new LinkedHashMap<>();

        for (ImportTargetEnum target : ImportTargetEnum.values()) {
            List<ImportSession> sessions = importSessionRepository.findAll((root, cq, cb) ->
                cb.equal(root.get("target"), target.name()));
            result.put(target.name().toLowerCase(), buildTargetStats(target.name(), sessions));
        }

        return result;
    }

    // ─── WSSI ANALYTICS ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getWssiAnalytics() {
        List<ImportedRecord> records = importedRecordRepository.findAll((root, cq, cb) -> {
            Subquery<Long> sessionSubquery = cq.subquery(Long.class);
            var sessionRoot = sessionSubquery.from(ImportSession.class);
            sessionSubquery.select(sessionRoot.get("id"))
                .where(cb.equal(sessionRoot.get("target"), "WSSI"));
            return cb.in(root.get("sessionId")).value(sessionSubquery);
        });

        BigDecimal totalReceived = BigDecimal.ZERO;
        BigDecimal totalSold = BigDecimal.ZERO;
        List<Map<String, Object>> details = new ArrayList<>();

        for (ImportedRecord record : records) {
            try {
                Map<String, Object> data = objectMapper.readValue(record.getData(),
                    new TypeReference<Map<String, Object>>() {});

                Object received = data.get("received_qty");
                Object sold = data.get("sold_qty");

                if (received != null) {
                    totalReceived = totalReceived.add(toBigDecimal(received));
                }
                if (sold != null) {
                    totalSold = totalSold.add(toBigDecimal(sold));
                }

                details.add(data);
            } catch (JsonProcessingException e) {
                // Skip malformed records
            }
        }

        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("totalRecords", records.size());
        analytics.put("totalReceivedQty", totalReceived);
        analytics.put("totalSoldQty", totalSold);
        analytics.put("sellThroughRate", totalReceived.compareTo(BigDecimal.ZERO) > 0
            ? totalSold.divide(totalReceived, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO);
        analytics.put("details", details);

        return analytics;
    }

    // ─── DELETE SESSION ────────────────────────────────────────────────────────

    public Long deleteSession(String target, String sessionId) {
        Optional<ImportSession> sessionOpt = importSessionRepository.findByFileName(sessionId);

        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        ImportSession session = sessionOpt.get();

        if (target != null && !target.isBlank() && !session.getTarget().equals(target)) {
            throw new IllegalArgumentException("Session does not match target: " + target);
        }

        Long sessionIdLong = session.getId();

        // Delete all imported records for this session
        importedRecordRepository.deleteBySessionId(sessionIdLong);

        // Delete the session
        importSessionRepository.delete(session);

        return sessionIdLong;
    }

    // ─── CLEAR ALL ────────────────────────────────────────────────────────────

    public Long clearAll(String target) {
        List<ImportSession> sessions = importSessionRepository.findAll((root, cq, cb) ->
            cb.equal(root.get("target"), target));

        if (sessions.isEmpty()) {
            return 0L;
        }

        for (ImportSession session : sessions) {
            importedRecordRepository.deleteBySessionId(session.getId());
            importSessionRepository.delete(session);
        }

        return (long) sessions.size();
    }

    // ─── APPLY IMPORTED DATA ──────────────────────────────────────────────────

    public ApplyResult applyImportedData(String target, String sessionId) {
        if (!"PRODUCTS".equalsIgnoreCase(target)) {
            return new ApplyResult(0, 0, List.of("Apply is only implemented for PRODUCTS target"));
        }

        ImportSession session;
        if (sessionId != null && !sessionId.isBlank()) {
            session = importSessionRepository.findByFileName(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        } else {
            // Get the most recent session for this target
            List<ImportSession> sessions = importSessionRepository.findAll((root, cq, cb) ->
                cb.equal(root.get("target"), target));
            if (sessions.isEmpty()) {
                return new ApplyResult(0, 0, List.of("No sessions found for target: " + target));
            }
            session = sessions.get(sessions.size() - 1);
        }

        List<ImportedRecord> records = importedRecordRepository.findBySessionId(session.getId());

        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (ImportedRecord record : records) {
            try {
                Map<String, Object> data = objectMapper.readValue(record.getData(),
                    new TypeReference<Map<String, Object>>() {});

                upsertProduct(data, session.getCreatedBy());
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("Row " + record.getRowNumber() + ": " + e.getMessage());
            }
        }

        return new ApplyResult(success, failed, errors);
    }

    // ─── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private ImportSession getOrCreateSession(String sessionId, ImportTargetEnum target, Long userId) {
        if (sessionId != null && !sessionId.isBlank()) {
            Optional<ImportSession> existing = importSessionRepository.findByFileName(sessionId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        ImportSession session = ImportSession.builder()
            .target(target.name())
            .fileName(sessionId != null ? sessionId : UUID.randomUUID().toString())
            .status("PROCESSING")
            .totalRows(0)
            .successRows(0)
            .errorRows(0)
            .createdBy(userId)
            .createdAt(Instant.now())
            .build();

        return importSessionRepository.save(session);
    }

    private Map<String, Object> buildTargetStats(String target, List<ImportSession> sessions) {
        int totalSessions = sessions.size();
        int totalRows = sessions.stream()
            .mapToInt(s -> s.getTotalRows() != null ? s.getTotalRows() : 0)
            .sum();
        int successRows = sessions.stream()
            .mapToInt(s -> s.getSuccessRows() != null ? s.getSuccessRows() : 0)
            .sum();
        int errorRows = sessions.stream()
            .mapToInt(s -> s.getErrorRows() != null ? s.getErrorRows() : 0)
            .sum();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("target", target);
        stats.put("totalSessions", totalSessions);
        stats.put("totalRows", totalRows);
        stats.put("successRows", successRows);
        stats.put("errorRows", errorRows);
        stats.put("sessions", sessions.stream()
            .map(this::sessionToMap)
            .toList());

        return stats;
    }

    private Map<String, Object> recordToMap(ImportedRecord record) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", record.getId());
        map.put("sessionId", record.getSessionId());
        map.put("rowNumber", record.getRowNumber());
        map.put("data", record.getData());
        map.put("status", record.getStatus());
        map.put("error", record.getError());
        map.put("createdAt", record.getCreatedAt());
        return map;
    }

    private Map<String, Object> sessionToMap(ImportSession session) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", session.getId());
        map.put("target", session.getTarget());
        map.put("fileName", session.getFileName());
        map.put("status", session.getStatus());
        map.put("totalRows", session.getTotalRows());
        map.put("successRows", session.getSuccessRows());
        map.put("errorRows", session.getErrorRows());
        map.put("createdBy", session.getCreatedBy());
        map.put("createdAt", session.getCreatedAt());
        map.put("updatedAt", session.getUpdatedAt());
        return map;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void upsertProduct(Map<String, Object> data, Long userId) {
        String skuCode = (String) data.get("sku_code");
        if (skuCode == null || skuCode.isBlank()) {
            throw new IllegalArgumentException("sku_code is required");
        }

        Optional<Product> existing = productRepository.findAll((root, cq, cb) ->
            cb.equal(root.get("skuCode"), skuCode)).stream().findFirst();

        if (existing.isPresent()) {
            Product product = existing.get();
            if (data.get("product_name") != null) {
                product.setProductName((String) data.get("product_name"));
            }
            if (data.get("sub_category_id") != null) {
                product.setSubCategoryId(toLong(data.get("sub_category_id")));
            }
            if (data.get("brand_id") != null) {
                product.setBrandId(toLong(data.get("brand_id")));
            }
            if (data.get("family") != null) {
                product.setFamily((String) data.get("family"));
            }
            if (data.get("theme") != null) {
                product.setTheme((String) data.get("theme"));
            }
            if (data.get("color") != null) {
                product.setColor((String) data.get("color"));
            }
            if (data.get("composition") != null) {
                product.setComposition((String) data.get("composition"));
            }
            if (data.get("srp") != null) {
                product.setSrp(toBigDecimal(data.get("srp")));
            }
            if (data.get("image_url") != null) {
                product.setImageUrl((String) data.get("image_url"));
            }
            if (data.get("is_active") != null) {
                product.setIsActive(Boolean.valueOf(data.get("is_active").toString()));
            }
            product.setUpdatedBy(userId);
            product.setUpdatedAt(Instant.now());
            productRepository.save(product);
        } else {
            Product product = Product.builder()
                .skuCode(skuCode)
                .productName((String) data.get("product_name"))
                .subCategoryId(toLong(data.get("sub_category_id")))
                .brandId(toLong(data.get("brand_id")))
                .family((String) data.get("family"))
                .theme((String) data.get("theme"))
                .color((String) data.get("color"))
                .composition((String) data.get("composition"))
                .srp(toBigDecimal(data.get("srp")))
                .imageUrl((String) data.get("image_url"))
                .isActive(data.get("is_active") != null ? Boolean.valueOf(data.get("is_active").toString()) : true)
                .createdBy(userId)
                .createdAt(Instant.now())
                .build();
            productRepository.save(product);
        }
    }
}
