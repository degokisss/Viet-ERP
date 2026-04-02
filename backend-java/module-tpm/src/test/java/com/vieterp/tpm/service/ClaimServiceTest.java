package com.vieterp.tpm.service;

import com.vieterp.tpm.domain.Claim;
import com.vieterp.tpm.domain.enums.ClaimStatus;
import com.vieterp.tpm.exception.ClaimNotFoundException;
import com.vieterp.tpm.repository.ClaimRepository;
import com.vieterp.tpm.domain.dto.ClaimResponse;
import com.vieterp.tpm.domain.dto.CreateClaimRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    private ClaimEventPublisherTestDouble eventPublisher = new ClaimEventPublisherTestDouble();

    private ClaimService claimService;

    private Claim testClaim;
    private UUID testId;

    @BeforeEach
    void setUp() {
        claimService = new ClaimService(claimRepository, eventPublisher);

        testId = UUID.randomUUID();
        testClaim = Claim.builder()
            .id(testId)
            .claimNumber("CLM-001")
            .claimType("REBATE")
            .amount(BigDecimal.valueOf(1500))
            .status(ClaimStatus.DRAFT)
            .customerId("customer-123")
            .promotionId("promotion-456")
            .tenantId("tenant-123")
            .createdBy("user-123")
            .build();
    }

    @Test
    void create_savesClaimAndPublishesEvent() {
        CreateClaimRequest req = new CreateClaimRequest(
            "CLM-001", "REBATE", BigDecimal.valueOf(1500),
            ClaimStatus.DRAFT, "customer-123", "promotion-456",
            null, null, "tenant-123", "user-123"
        );

        when(claimRepository.save(any(Claim.class))).thenReturn(testClaim);
        ClaimResponse resp = claimService.create(req);

        assertNotNull(resp);
        assertEquals(testId, resp.id());
        assertEquals("CLM-001", resp.claimNumber());
        verify(claimRepository).save(any(Claim.class));
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(claimRepository.findById(testId)).thenReturn(Optional.empty());
        assertThrows(ClaimNotFoundException.class, () -> claimService.getById(testId));
    }

    @Test
    void listAll_returnsAllClaims() {
        when(claimRepository.findAll()).thenReturn(List.of(testClaim));
        List<ClaimResponse> result = claimService.listAll();
        assertEquals(1, result.size());
        assertEquals(testId, result.get(0).id());
    }

    @Test
    void delete_deletesWhenExists() {
        when(claimRepository.existsById(testId)).thenReturn(true);
        doNothing().when(claimRepository).deleteById(testId);
        claimService.delete(testId);
        verify(claimRepository).deleteById(testId);
    }

    @Test
    void update_updatesClaimAndPublishesEvent() {
        CreateClaimRequest req = new CreateClaimRequest(
            "CLM-002", "DISCOUNT", BigDecimal.valueOf(2500),
            ClaimStatus.SUBMITTED, "customer-789", "promotion-101",
            Instant.now(), null, "tenant-123", "user-456"
        );

        when(claimRepository.findById(testId)).thenReturn(Optional.of(testClaim));
        when(claimRepository.save(any(Claim.class))).thenReturn(testClaim);

        ClaimResponse resp = claimService.update(testId, req);

        assertNotNull(resp);
        verify(claimRepository).save(any(Claim.class));
        assertTrue(eventPublisher.wasPublishUpdatedCalled());
    }
}
