package com.vieterp.tpm.event;

import com.vieterp.tpm.domain.Claim;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimEventPublisher {

    private final StreamBridge streamBridge;

    public void publishCreated(Claim claim) {
        boolean sent = streamBridge.send("tpm-claim-created-out-0",
            new ClaimEvent(claim.getId(), claim.getClaimNumber(), claim.getAmount(), claim.getTenantId(), Instant.now()));
        if (!sent) log.warn("Event not sent for claim {} — no binder bound", claim.getId());
        else log.info("Published ClaimCreatedEvent for {}", claim.getId());
    }

    public void publishUpdated(Claim claim) {
        boolean sent = streamBridge.send("tpm-claim-updated-out-0",
            new ClaimEvent(claim.getId(), claim.getClaimNumber(), claim.getAmount(), claim.getTenantId(), Instant.now()));
        if (!sent) log.warn("Event not sent for claim {} — no binder bound", claim.getId());
        else log.info("Published ClaimUpdatedEvent for {}", claim.getId());
    }

    public void publishDeleted(UUID claimId) {
        boolean sent = streamBridge.send("tpm-claim-deleted-out-0",
            new ClaimDeletedEvent(claimId, Instant.now()));
        if (!sent) log.warn("Event not sent for claim {} — no binder bound", claimId);
        else log.info("Published ClaimDeletedEvent for {}", claimId);
    }

    public record ClaimEvent(UUID id, String claimNumber, BigDecimal amount, String tenantId, Instant timestamp) {}
    public record ClaimDeletedEvent(UUID id, Instant timestamp) {}
}
