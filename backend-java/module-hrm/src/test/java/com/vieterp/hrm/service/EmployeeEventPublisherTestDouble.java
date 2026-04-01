package com.vieterp.hrm.service;

import com.vieterp.hrm.domain.Employee;
import java.util.UUID;

/**
 * Test double for EmployeeEventPublisher to avoid StreamBridge dependency in tests.
 */
class EmployeeEventPublisherTestDouble extends com.vieterp.hrm.event.EmployeeEventPublisher {

    private boolean publishUpdatedCalled = false;

    public EmployeeEventPublisherTestDouble() {
        super(null);
    }

    @Override
    public void publishCreated(Employee employee) {
        // no-op in tests
    }

    @Override
    public void publishUpdated(Employee employee) {
        publishUpdatedCalled = true;
    }

    @Override
    public void publishDeleted(UUID employeeId) {
        // no-op in tests
    }

    public boolean wasPublishUpdatedCalled() {
        return publishUpdatedCalled;
    }

    public void reset() {
        publishUpdatedCalled = false;
    }
}
