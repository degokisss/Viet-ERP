package com.vieterp.tpm.event;

import com.vieterp.tpm.domain.Promotion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionEventPublisher {

    private final StreamBridge streamBridge;

    public void publishCreated(Promotion promotion) {
        boolean sent = streamBridge.send("tpm-promotion-created-out-0",
            new PromotionEvent(promotion.getId(), promotion.getName(), promotion.getTenantId(), Instant.now()));
        if (!sent) log.warn("Event not sent for promotion {} — no binder bound", promotion.getId());
        else log.info("Published PromotionCreatedEvent for {}", promotion.getId());
    }

    public void publishUpdated(Promotion promotion) {
        boolean sent = streamBridge.send("tpm-promotion-updated-out-0",
            new PromotionEvent(promotion.getId(), promotion.getName(), promotion.getTenantId(), Instant.now()));
        if (!sent) log.warn("Event not sent for promotion {} — no binder bound", promotion.getId());
        else log.info("Published PromotionUpdatedEvent for {}", promotion.getId());
    }

    public void publishDeleted(UUID promotionId) {
        boolean sent = streamBridge.send("tpm-promotion-deleted-out-0",
            new PromotionDeletedEvent(promotionId, Instant.now()));
        if (!sent) log.warn("Event not sent for promotion {} — no binder bound", promotionId);
        else log.info("Published PromotionDeletedEvent for {}", promotionId);
    }

    public record PromotionEvent(UUID id, String name, String tenantId, Instant timestamp) {}
    public record PromotionDeletedEvent(UUID id, Instant timestamp) {}
}
