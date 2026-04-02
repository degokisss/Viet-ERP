package com.vieterp.tpm.service;

import com.vieterp.tpm.domain.Claim;
import java.util.UUID;

/**
 * Test double for ClaimEventPublisher to avoid StreamBridge dependency in tests.
 */
class ClaimEventPublisherTestDouble extends com.vieterp.tpm.event.ClaimEventPublisher {

    private boolean publishUpdatedCalled = false;

    public ClaimEventPublisherTestDouble() {
        super(null);
    }

    @Override
    public void publishCreated(Claim claim) {
        // no-op in tests
    }

    @Override
    public void publishUpdated(Claim claim) {
        publishUpdatedCalled = true;
    }

    @Override
    public void publishDeleted(UUID claimId) {
        // no-op in tests
    }

    public boolean wasPublishUpdatedCalled() {
        return publishUpdatedCalled;
    }

    public void reset() {
        publishUpdatedCalled = false;
    }
}
