package com.vieterp.crm.event;

import com.vieterp.crm.domain.Company;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyEventPublisher {

    private final StreamBridge streamBridge;

    public void publishCreated(Company company) {
        boolean sent = streamBridge.send("crm-company-created-out-0", new CompanyEvent(company.getId(), company.getName(), company.getOwnerId(), Instant.now()));
        if (!sent) log.warn("Event not sent for company {} — no binder bound", company.getId());
        else log.info("Published CompanyCreatedEvent for {}", company.getId());
    }

    public void publishUpdated(Company company) {
        boolean sent = streamBridge.send("crm-company-updated-out-0", new CompanyEvent(company.getId(), company.getName(), company.getOwnerId(), Instant.now()));
        if (!sent) log.warn("Event not sent for company {} — no binder bound", company.getId());
        else log.info("Published CompanyUpdatedEvent for {}", company.getId());
    }

    public void publishDeleted(UUID companyId) {
        boolean sent = streamBridge.send("crm-company-deleted-out-0", new CompanyDeletedEvent(companyId, Instant.now()));
        if (!sent) log.warn("Event not sent for company {} — no binder bound", companyId);
        else log.info("Published CompanyDeletedEvent for {}", companyId);
    }

    public record CompanyEvent(UUID id, String name, String ownerId, Instant timestamp) {}
    public record CompanyDeletedEvent(UUID id, Instant timestamp) {}
}
