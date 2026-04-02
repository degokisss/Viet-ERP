package com.vieterp.crm.service;

import com.vieterp.crm.domain.Company;
import java.util.UUID;

/**
 * Test double for CompanyEventPublisher to avoid StreamBridge dependency in tests.
 */
class CompanyEventPublisherTestDouble extends com.vieterp.crm.event.CompanyEventPublisher {

    private boolean publishUpdatedCalled = false;

    public CompanyEventPublisherTestDouble() {
        super(null);
    }

    @Override
    public void publishCreated(Company company) {
        // no-op in tests
    }

    @Override
    public void publishUpdated(Company company) {
        publishUpdatedCalled = true;
    }

    @Override
    public void publishDeleted(UUID companyId) {
        // no-op in tests
    }

    public boolean wasPublishUpdatedCalled() {
        return publishUpdatedCalled;
    }

    public void reset() {
        publishUpdatedCalled = false;
    }
}
