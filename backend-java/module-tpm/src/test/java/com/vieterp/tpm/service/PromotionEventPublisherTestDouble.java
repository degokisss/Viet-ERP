package com.vieterp.tpm.service;

import com.vieterp.tpm.domain.Promotion;
import java.util.UUID;

/**
 * Test double for PromotionEventPublisher to avoid StreamBridge dependency in tests.
 */
class PromotionEventPublisherTestDouble extends com.vieterp.tpm.event.PromotionEventPublisher {

    private boolean publishUpdatedCalled = false;

    public PromotionEventPublisherTestDouble() {
        super(null);
    }

    @Override
    public void publishCreated(Promotion promotion) {
        // no-op in tests
    }

    @Override
    public void publishUpdated(Promotion promotion) {
        publishUpdatedCalled = true;
    }

    @Override
    public void publishDeleted(UUID promotionId) {
        // no-op in tests
    }

    public boolean wasPublishUpdatedCalled() {
        return publishUpdatedCalled;
    }

    public void reset() {
        publishUpdatedCalled = false;
    }
}
