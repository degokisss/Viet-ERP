package com.vieterp.mrp.service;

import com.vieterp.mrp.domain.BomHeader;
import java.util.UUID;

/**
 * Test double for BomHeaderEventPublisher to avoid StreamBridge dependency in tests.
 */
class BomHeaderEventPublisherTestDouble extends com.vieterp.mrp.event.BomHeaderEventPublisher {

    private boolean publishUpdatedCalled = false;

    public BomHeaderEventPublisherTestDouble() {
        super(null);
    }

    @Override
    public void publishCreated(BomHeader bom) {
        // no-op in tests
    }

    @Override
    public void publishUpdated(BomHeader bom) {
        publishUpdatedCalled = true;
    }

    @Override
    public void publishDeleted(UUID bomId) {
        // no-op in tests
    }

    public boolean wasPublishUpdatedCalled() {
        return publishUpdatedCalled;
    }

    public void reset() {
        publishUpdatedCalled = false;
    }
}
