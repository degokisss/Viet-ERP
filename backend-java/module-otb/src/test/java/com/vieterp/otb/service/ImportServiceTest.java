package com.vieterp.otb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vieterp.otb.domain.*;
import com.vieterp.otb.domain.repository.ProductRepository;
import com.vieterp.otb.imports.ImportService;
import com.vieterp.otb.imports.dto.*;
import com.vieterp.otb.imports.repository.ImportedRecordRepository;
import com.vieterp.otb.imports.repository.ImportSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.jpa.domain.Specification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImportServiceTest {

    @Mock private ImportSessionRepository importSessionRepository;
    @Mock private ImportedRecordRepository importedRecordRepository;
    @Mock private ProductRepository productRepository;

    private ObjectMapper objectMapper;
    private ImportService importService;

    private ImportSession testSession;
    private ImportedRecord testRecord;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        importService = new ImportService(
            importSessionRepository,
            importedRecordRepository,
            productRepository,
            objectMapper
        );

        testSession = ImportSession.builder()
            .id(1L)
            .target("PRODUCTS")
            .fileName("session-001")
            .status("PROCESSING")
            .totalRows(0)
            .successRows(0)
            .errorRows(0)
            .createdBy(1L)
            .createdAt(Instant.now())
            .build();

        testRecord = ImportedRecord.builder()
            .id(1L)
            .sessionId(1L)
            .rowNumber(1)
            .data("{\"sku_code\":\"SKU001\",\"product_name\":\"Test Product\"}")
            .status("PENDING")
            .createdAt(Instant.now())
            .build();
    }

    // ─── PROCESS BATCH ─────────────────────────────────────────────────────────

    @Test
    void processBatch_savesRecordsSuccessfully() {
        ImportBatchDto dto = ImportBatchDto.builder()
            .target(ImportTargetEnum.PRODUCTS)
            .sessionId("session-001")
            .rows(List.of(Map.of("sku_code", "SKU001", "product_name", "Test Product")))
            .batchIndex(0)
            .totalBatches(1)
            .build();

        when(importSessionRepository.findByFileName("session-001")).thenReturn(Optional.of(testSession));
        when(importedRecordRepository.save(any(ImportedRecord.class))).thenAnswer(inv -> {
            ImportedRecord r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = importService.processBatch(dto, 1L);

        assertNotNull(result);
        assertEquals(1L, result.get("sessionId"));
        assertEquals(1, result.get("processed"));
        assertEquals(1, result.get("success"));
        assertEquals(0, result.get("errors"));
        verify(importedRecordRepository, times(1)).save(any(ImportedRecord.class));
        verify(importSessionRepository, times(1)).save(any(ImportSession.class));
    }

    @Test
    void processBatch_createsNewSessionWhenNotFound() {
        ImportBatchDto dto = ImportBatchDto.builder()
            .target(ImportTargetEnum.PRODUCTS)
            .sessionId("new-session")
            .rows(List.of(Map.of("sku_code", "SKU001")))
            .batchIndex(0)
            .totalBatches(1)
            .build();

        when(importSessionRepository.findByFileName("new-session")).thenReturn(Optional.empty());
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(inv -> {
            ImportSession s = inv.getArgument(0);
            s.setId(2L);
            return s;
        });
        when(importedRecordRepository.save(any(ImportedRecord.class))).thenAnswer(inv -> {
            ImportedRecord r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        Map<String, Object> result = importService.processBatch(dto, 1L);

        assertNotNull(result);
        assertEquals(2L, result.get("sessionId"));
        verify(importSessionRepository, times(2)).save(any(ImportSession.class));
        verify(importedRecordRepository, times(1)).save(any(ImportedRecord.class));
    }

    // ─── QUERY DATA ────────────────────────────────────────────────────────────

    @Test
    void queryData_returnsPaginatedResults() {
        ImportQueryDto query = new ImportQueryDto();
        query.setTarget(ImportTargetEnum.PRODUCTS);
        query.setPage(1);
        query.setPageSize(20);
        query.setSortBy("createdAt");
        query.setSortOrder("DESC");

        Page<ImportedRecord> page = new PageImpl<>(
            List.of(testRecord),
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
            1
        );

        when(importedRecordRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);

        Map<String, Object> result = importService.queryData(query);

        assertNotNull(result);
        assertNotNull(result.get("data"));
        assertNotNull(result.get("meta"));
        verify(importedRecordRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void queryData_withSearchTerm_buildsSearchSpecification() {
        ImportQueryDto query = new ImportQueryDto();
        query.setSearch("SKU001");
        query.setPage(1);
        query.setPageSize(20);
        query.setSortBy("createdAt");
        query.setSortOrder("ASC");

        Page<ImportedRecord> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(importedRecordRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(emptyPage);

        Map<String, Object> result = importService.queryData(query);

        assertNotNull(result);
        verify(importedRecordRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // ─── GET STATS ─────────────────────────────────────────────────────────────

    @Test
    void getStats_aggregatesCorrectly() {
        ImportSession session1 = ImportSession.builder()
            .id(1L)
            .target("PRODUCTS")
            .fileName("file1")
            .status("COMPLETED")
            .totalRows(100)
            .successRows(95)
            .errorRows(5)
            .createdAt(Instant.now())
            .build();
        ImportSession session2 = ImportSession.builder()
            .id(2L)
            .target("PRODUCTS")
            .fileName("file2")
            .status("COMPLETED")
            .totalRows(50)
            .successRows(50)
            .errorRows(0)
            .createdAt(Instant.now())
            .build();

        when(importSessionRepository.findAll(any(Specification.class)))
            .thenReturn(List.of(session1, session2));

        Map<String, Object> result = importService.getStats("PRODUCTS");

        assertNotNull(result);
        assertEquals("PRODUCTS", result.get("target"));
        assertEquals(2, result.get("totalSessions"));
        assertEquals(150, result.get("totalRows"));
        assertEquals(145, result.get("successRows"));
        assertEquals(5, result.get("errorRows"));
    }

    @Test
    void getStats_emptyTarget_returnsAllTargetStats() {
        when(importSessionRepository.findAll(any(Specification.class)))
            .thenReturn(List.of());

        Map<String, Object> result = importService.getStats(null);

        assertNotNull(result);
        verify(importSessionRepository, times(ImportTargetEnum.values().length))
            .findAll(any(Specification.class));
    }

    // ─── DELETE SESSION ────────────────────────────────────────────────────────

    @Test
    void deleteSession_removesSessionAndRecords() {
        when(importSessionRepository.findByFileName("session-001"))
            .thenReturn(Optional.of(testSession));
        doNothing().when(importedRecordRepository).deleteBySessionId(1L);
        doNothing().when(importSessionRepository).delete(testSession);

        Long deletedId = importService.deleteSession("PRODUCTS", "session-001");

        assertEquals(1L, deletedId);
        verify(importedRecordRepository).deleteBySessionId(1L);
        verify(importSessionRepository).delete(testSession);
    }

    @Test
    void deleteSession_notFound_throwsException() {
        when(importSessionRepository.findByFileName("nonexistent"))
            .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> importService.deleteSession("PRODUCTS", "nonexistent"));
    }

    @Test
    void deleteSession_targetMismatch_throwsException() {
        when(importSessionRepository.findByFileName("session-001"))
            .thenReturn(Optional.of(testSession));

        assertThrows(IllegalArgumentException.class,
            () -> importService.deleteSession("WSSI", "session-001"));
    }

    // ─── CLEAR ALL ────────────────────────────────────────────────────────────

    @Test
    void clearAll_removesAllSessionsForTarget() {
        ImportSession session1 = ImportSession.builder()
            .id(1L)
            .target("PRODUCTS")
            .fileName("file1")
            .status("COMPLETED")
            .createdAt(Instant.now())
            .build();
        ImportSession session2 = ImportSession.builder()
            .id(2L)
            .target("PRODUCTS")
            .fileName("file2")
            .status("COMPLETED")
            .createdAt(Instant.now())
            .build();

        when(importSessionRepository.findAll(any(Specification.class)))
            .thenReturn(List.of(session1, session2));
        doNothing().when(importedRecordRepository).deleteBySessionId(anyLong());
        doNothing().when(importSessionRepository).delete(any(ImportSession.class));

        Long cleared = importService.clearAll("PRODUCTS");

        assertEquals(2L, cleared);
        verify(importedRecordRepository, times(2)).deleteBySessionId(anyLong());
        verify(importSessionRepository, times(2)).delete(any(ImportSession.class));
    }

    @Test
    void clearAll_noSessionsFound_returnsZero() {
        when(importSessionRepository.findAll(any(Specification.class)))
            .thenReturn(List.of());

        Long cleared = importService.clearAll("PRODUCTS");

        assertEquals(0L, cleared);
        verify(importSessionRepository, never()).delete(any(ImportSession.class));
    }

    // ─── APPLY IMPORTED DATA ──────────────────────────────────────────────────

    @Test
    void applyImportedData_productsTarget_upsertsNewProduct() {
        ImportSession session = ImportSession.builder()
            .id(1L)
            .target("PRODUCTS")
            .fileName("session-001")
            .status("COMPLETED")
            .createdBy(1L)
            .createdAt(Instant.now())
            .build();

        ImportedRecord record = ImportedRecord.builder()
            .id(1L)
            .sessionId(1L)
            .rowNumber(1)
            .data("{\"sku_code\":\"SKU001\",\"product_name\":\"New Product\",\"srp\":\"99.99\"}")
            .status("PENDING")
            .createdAt(Instant.now())
            .build();

        when(importSessionRepository.findByFileName("session-001"))
            .thenReturn(Optional.of(session));
        when(importedRecordRepository.findBySessionId(1L))
            .thenReturn(List.of(record));
        when(productRepository.findAll(any(Specification.class)))
            .thenReturn(List.of());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportService.ApplyResult result = importService.applyImportedData("PRODUCTS", "session-001");

        assertEquals(1, result.success());
        assertEquals(0, result.failed());
        assertTrue(result.errors().isEmpty());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void applyImportedData_productsTarget_updatesExistingProduct() {
        ImportSession session = ImportSession.builder()
            .id(1L)
            .target("PRODUCTS")
            .fileName("session-001")
            .status("COMPLETED")
            .createdBy(1L)
            .createdAt(Instant.now())
            .build();

        Product existingProduct = Product.builder()
            .id(10L)
            .skuCode("SKU001")
            .productName("Old Name")
            .isActive(true)
            .createdBy(1L)
            .createdAt(Instant.now())
            .build();

        ImportedRecord record = ImportedRecord.builder()
            .id(1L)
            .sessionId(1L)
            .rowNumber(1)
            .data("{\"sku_code\":\"SKU001\",\"product_name\":\"Updated Name\",\"is_active\":false}")
            .status("PENDING")
            .createdAt(Instant.now())
            .build();

        when(importSessionRepository.findByFileName("session-001"))
            .thenReturn(Optional.of(session));
        when(importedRecordRepository.findBySessionId(1L))
            .thenReturn(List.of(record));
        when(productRepository.findAll(any(Specification.class)))
            .thenReturn(List.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportService.ApplyResult result = importService.applyImportedData("PRODUCTS", "session-001");

        assertEquals(1, result.success());
        assertEquals(0, result.failed());
        verify(productRepository, times(1)).save(argThat(p ->
            "Updated Name".equals(p.getProductName()) && Boolean.FALSE.equals(p.getIsActive())
        ));
    }

    @Test
    void applyImportedData_nonProductsTarget_returnsError() {
        ImportService.ApplyResult result = importService.applyImportedData("WSSI", null);

        assertEquals(0, result.success());
        assertEquals(0, result.failed());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).contains("only implemented for PRODUCTS"));
    }

    @Test
    void applyImportedData_sessionNotFound_throwsException() {
        when(importSessionRepository.findByFileName("nonexistent"))
            .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> importService.applyImportedData("PRODUCTS", "nonexistent"));
    }

    @Test
    void applyImportedData_noSessionIdUsesMostRecentSession() {
        ImportSession recentSession = ImportSession.builder()
            .id(2L)
            .target("PRODUCTS")
            .fileName("recent-session")
            .status("COMPLETED")
            .createdBy(1L)
            .createdAt(Instant.now())
            .build();

        ImportSession olderSession = ImportSession.builder()
            .id(1L)
            .target("PRODUCTS")
            .fileName("older-session")
            .status("COMPLETED")
            .createdBy(1L)
            .createdAt(Instant.now().minusSeconds(3600))
            .build();

        ImportedRecord record = ImportedRecord.builder()
            .id(1L)
            .sessionId(2L)
            .rowNumber(1)
            .data("{\"sku_code\":\"SKU001\",\"product_name\":\"Product\"}")
            .status("PENDING")
            .createdAt(Instant.now())
            .build();

        when(importSessionRepository.findAll(any(Specification.class)))
            .thenReturn(List.of(olderSession, recentSession));
        when(importedRecordRepository.findBySessionId(2L))
            .thenReturn(List.of(record));
        when(productRepository.findAll(any(Specification.class)))
            .thenReturn(List.of());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportService.ApplyResult result = importService.applyImportedData("PRODUCTS", null);

        assertEquals(1, result.success());
        verify(importedRecordRepository).findBySessionId(2L);
    }

    @Test
    void applyImportedData_missingSkuCode_returnsFailed() {
        ImportSession session = ImportSession.builder()
            .id(1L)
            .target("PRODUCTS")
            .fileName("session-001")
            .status("COMPLETED")
            .createdBy(1L)
            .createdAt(Instant.now())
            .build();

        ImportedRecord record = ImportedRecord.builder()
            .id(1L)
            .sessionId(1L)
            .rowNumber(1)
            .data("{\"product_name\":\"No SKU\"}")
            .status("PENDING")
            .createdAt(Instant.now())
            .build();

        when(importSessionRepository.findByFileName("session-001"))
            .thenReturn(Optional.of(session));
        when(importedRecordRepository.findBySessionId(1L))
            .thenReturn(List.of(record));

        ImportService.ApplyResult result = importService.applyImportedData("PRODUCTS", "session-001");

        assertEquals(0, result.success());
        assertEquals(1, result.failed());
        assertTrue(result.errors().get(0).contains("sku_code is required"));
    }
}
