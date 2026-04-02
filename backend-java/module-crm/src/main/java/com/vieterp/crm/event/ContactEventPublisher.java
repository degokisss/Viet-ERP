package com.vieterp.crm.event;

import com.vieterp.crm.domain.Contact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContactEventPublisher {

    private final StreamBridge streamBridge;

    public void publishCreated(Contact contact) {
        boolean sent = streamBridge.send("crm-contact-created-out-0", new ContactEvent(contact.getId(), contact.getEmail(), contact.getFirstName(), contact.getLastName(), contact.getOwnerId(), Instant.now()));
        if (!sent) log.warn("Event not sent for contact {} — no binder bound", contact.getId());
        else log.info("Published ContactCreatedEvent for {}", contact.getId());
    }

    public void publishUpdated(Contact contact) {
        boolean sent = streamBridge.send("crm-contact-updated-out-0", new ContactEvent(contact.getId(), contact.getEmail(), contact.getFirstName(), contact.getLastName(), contact.getOwnerId(), Instant.now()));
        if (!sent) log.warn("Event not sent for contact {} — no binder bound", contact.getId());
        else log.info("Published ContactUpdatedEvent for {}", contact.getId());
    }

    public void publishDeleted(UUID contactId) {
        boolean sent = streamBridge.send("crm-contact-deleted-out-0", new ContactDeletedEvent(contactId, Instant.now()));
        if (!sent) log.warn("Event not sent for contact {} — no binder bound", contactId);
        else log.info("Published ContactDeletedEvent for {}", contactId);
    }

    public record ContactEvent(UUID id, String email, String firstName, String lastName, String ownerId, Instant timestamp) {}
    public record ContactDeletedEvent(UUID id, Instant timestamp) {}
}
