package com.vieterp.crm.service;

import com.vieterp.crm.domain.Contact;
import java.util.UUID;

/**
 * Test double for ContactEventPublisher to avoid StreamBridge dependency in tests.
 */
class ContactEventPublisherTestDouble extends com.vieterp.crm.event.ContactEventPublisher {

    private boolean publishUpdatedCalled = false;

    public ContactEventPublisherTestDouble() {
        super(null);
    }

    @Override
    public void publishCreated(Contact contact) {
        // no-op in tests
    }

    @Override
    public void publishUpdated(Contact contact) {
        publishUpdatedCalled = true;
    }

    @Override
    public void publishDeleted(UUID contactId) {
        // no-op in tests
    }

    public boolean wasPublishUpdatedCalled() {
        return publishUpdatedCalled;
    }

    public void reset() {
        publishUpdatedCalled = false;
    }
}
