package com.vieterp.accounting.event;

import com.vieterp.accounting.domain.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEventPublisher {

    private final StreamBridge streamBridge;

    public void publishCreated(Account account) {
        boolean sent = streamBridge.send("accounting-account-created-out-0", new AccountEvent(account.getId(), account.getAccountNumber(), account.getName(), account.getTenantId(), Instant.now()));
        if (!sent) log.warn("Event not sent for account {} — no binder bound", account.getId());
        else log.info("Published AccountCreatedEvent for {}", account.getId());
    }

    public void publishUpdated(Account account) {
        boolean sent = streamBridge.send("accounting-account-updated-out-0", new AccountEvent(account.getId(), account.getAccountNumber(), account.getName(), account.getTenantId(), Instant.now()));
        if (!sent) log.warn("Event not sent for account {} — no binder bound", account.getId());
        else log.info("Published AccountUpdatedEvent for {}", account.getId());
    }

    public void publishDeleted(UUID accountId) {
        boolean sent = streamBridge.send("accounting-account-deleted-out-0", new AccountDeletedEvent(accountId, Instant.now()));
        if (!sent) log.warn("Event not sent for account {} — no binder bound", accountId);
        else log.info("Published AccountDeletedEvent for {}", accountId);
    }

    public record AccountEvent(UUID id, String accountNumber, String name, String tenantId, Instant timestamp) {}
    public record AccountDeletedEvent(UUID id, Instant timestamp) {}
}
