package com.vieterp.mrp.event;

import com.vieterp.mrp.domain.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PartEventPublisher {

    private final StreamBridge streamBridge;

    public void publishCreated(Part part) {
        boolean sent = streamBridge.send("mrp-part-created-out-0", new PartEvent(part.getId(), part.getPartNumber(), part.getName(), part.getTenantId(), Instant.now()));
        if (!sent) log.warn("Event not sent for part {} — no binder bound", part.getId());
        else log.info("Published PartCreatedEvent for {}", part.getId());
    }

    public void publishUpdated(Part part) {
        boolean sent = streamBridge.send("mrp-part-updated-out-0", new PartEvent(part.getId(), part.getPartNumber(), part.getName(), part.getTenantId(), Instant.now()));
        if (!sent) log.warn("Event not sent for part {} — no binder bound", part.getId());
        else log.info("Published PartUpdatedEvent for {}", part.getId());
    }

    public void publishDeleted(UUID partId) {
        boolean sent = streamBridge.send("mrp-part-deleted-out-0", new PartDeletedEvent(partId, Instant.now()));
        if (!sent) log.warn("Event not sent for part {} — no binder bound", partId);
        else log.info("Published PartDeletedEvent for {}", partId);
    }

    public record PartEvent(UUID id, String partNumber, String name, String tenantId, Instant timestamp) {}
    public record PartDeletedEvent(UUID id, Instant timestamp) {}
}
