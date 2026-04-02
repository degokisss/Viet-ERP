package com.vieterp.mrp.event;

import com.vieterp.mrp.domain.BomHeader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BomHeaderEventPublisher {

    private final StreamBridge streamBridge;

    public void publishCreated(BomHeader bom) {
        boolean sent = streamBridge.send("mrp-bom-created-out-0", new BomEvent(bom.getId(), bom.getBomNumber(), bom.getName(), bom.getTenantId(), Instant.now()));
        if (!sent) log.warn("Event not sent for BOM {} — no binder bound", bom.getId());
        else log.info("Published BomCreatedEvent for {}", bom.getId());
    }

    public void publishUpdated(BomHeader bom) {
        boolean sent = streamBridge.send("mrp-bom-updated-out-0", new BomEvent(bom.getId(), bom.getBomNumber(), bom.getName(), bom.getTenantId(), Instant.now()));
        if (!sent) log.warn("Event not sent for BOM {} — no binder bound", bom.getId());
        else log.info("Published BomUpdatedEvent for {}", bom.getId());
    }

    public void publishDeleted(UUID bomId) {
        boolean sent = streamBridge.send("mrp-bom-deleted-out-0", new BomDeletedEvent(bomId, Instant.now()));
        if (!sent) log.warn("Event not sent for BOM {} — no binder bound", bomId);
        else log.info("Published BomDeletedEvent for {}", bomId);
    }

    public record BomEvent(UUID id, String bomNumber, String name, String tenantId, Instant timestamp) {}
    public record BomDeletedEvent(UUID id, Instant timestamp) {}
}
