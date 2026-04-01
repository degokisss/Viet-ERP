package com.vieterp.hrm.event;

import com.vieterp.events.employee.EmployeeCreatedEvent;
import com.vieterp.events.employee.EmployeeDeletedEvent;
import com.vieterp.events.employee.EmployeeUpdatedEvent;
import com.vieterp.hrm.domain.Employee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeEventPublisher {

    private final StreamBridge streamBridge;

    public void publishCreated(Employee employee) {
        var event = new EmployeeCreatedEvent(
            employee.getId(), employee.getEmail(),
            employee.getFirstName(), employee.getLastName(), Instant.now());
        boolean sent = streamBridge.send("employee-created-out-0", event);
        if (!sent) log.warn("Event not sent for employee {} — no binder bound", employee.getId());
        else log.info("Published EmployeeCreatedEvent for {}", employee.getId());
    }

    public void publishUpdated(Employee employee) {
        var event = new EmployeeUpdatedEvent(
            employee.getId(), employee.getFirstName(),
            employee.getLastName(), employee.getEmail(), Instant.now());
        boolean sent = streamBridge.send("employee-updated-out-0", event);
        if (!sent) log.warn("Event not sent for employee {} — no binder bound", employee.getId());
        else log.info("Published EmployeeUpdatedEvent for {}", employee.getId());
    }

    public void publishDeleted(java.util.UUID employeeId) {
        var event = new EmployeeDeletedEvent(employeeId, Instant.now());
        boolean sent = streamBridge.send("employee-deleted-out-0", event);
        if (!sent) log.warn("Event not sent for employee {} — no binder bound", employeeId);
        else log.info("Published EmployeeDeletedEvent for {}", employeeId);
    }
}
