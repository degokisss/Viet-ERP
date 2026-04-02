package com.vieterp.mrp.service;

import com.vieterp.mrp.domain.Part;
import java.util.UUID;

/**
 * Test double for PartEventPublisher to avoid StreamBridge dependency in tests.
 */
class PartEventPublisherTestDouble extends com.vieterp.mrp.event.PartEventPublisher {

    private boolean publishUpdatedCalled = false;

    public PartEventPublisherTestDouble() {
        super(null);
    }

    @Override
    public void publishCreated(Part part) {
        // no-op in tests
    }

    @Override
    public void publishUpdated(Part part) {
        publishUpdatedCalled = true;
    }

    @Override
    public void publishDeleted(UUID partId) {
        // no-op in tests
    }

    public boolean wasPublishUpdatedCalled() {
        return publishUpdatedCalled;
    }

    public void reset() {
        publishUpdatedCalled = false;
    }
}
