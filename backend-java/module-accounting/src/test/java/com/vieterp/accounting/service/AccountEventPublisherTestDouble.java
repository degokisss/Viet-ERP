package com.vieterp.accounting.service;

import com.vieterp.accounting.domain.Account;
import java.util.UUID;

/**
 * Test double for AccountEventPublisher to avoid StreamBridge dependency in tests.
 */
class AccountEventPublisherTestDouble extends com.vieterp.accounting.event.AccountEventPublisher {

    private boolean publishUpdatedCalled = false;

    public AccountEventPublisherTestDouble() {
        super(null);
    }

    @Override
    public void publishCreated(Account account) {
        // no-op in tests
    }

    @Override
    public void publishUpdated(Account account) {
        publishUpdatedCalled = true;
    }

    @Override
    public void publishDeleted(UUID accountId) {
        // no-op in tests
    }

    public boolean wasPublishUpdatedCalled() {
        return publishUpdatedCalled;
    }

    public void reset() {
        publishUpdatedCalled = false;
    }
}
