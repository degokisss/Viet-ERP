package com.vieterp.crm.service;

import com.vieterp.crm.domain.Contact;
import com.vieterp.crm.exception.ContactNotFoundException;
import com.vieterp.crm.repository.CompanyRepository;
import com.vieterp.crm.repository.ContactRepository;
import com.vieterp.crm.service.dto.ContactResponse;
import com.vieterp.crm.service.dto.CreateContactRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock private ContactRepository contactRepository;
    @Mock private CompanyRepository companyRepository;

    private ContactEventPublisherTestDouble eventPublisher = new ContactEventPublisherTestDouble();

    private ContactService contactService;

    private Contact testContact;
    private UUID testId;

    @BeforeEach
    void setUp() {
        contactService = new ContactService(contactRepository, companyRepository, eventPublisher);

        testId = UUID.randomUUID();
        testContact = Contact.builder()
            .id(testId)
            .firstName("Nguyen")
            .lastName("Van A")
            .email("nguyenvana@vieterp.com")
            .phone("0912345678")
            .jobTitle("Developer")
            .status(Contact.ContactStatus.ACTIVE)
            .ownerId("owner-123")
            .score(50)
            .build();
    }

    @Test
    void create_savesContactAndPublishesEvent() {
        CreateContactRequest req = new CreateContactRequest(
            "Nguyen", "Van A", "nguyenvana@vieterp.com", "0912345678",
            "0901234567", "Developer", "IT", "https://avatar.url",
            "Notes", null, Contact.ContactStatus.ACTIVE, null, "VN", null, 50, "owner-123");

        when(contactRepository.save(any(Contact.class))).thenReturn(testContact);
        ContactResponse resp = contactService.create(req);

        assertNotNull(resp);
        assertEquals(testId, resp.id());
        assertEquals("Nguyen", resp.firstName());
        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(contactRepository.findById(testId)).thenReturn(Optional.empty());
        assertThrows(ContactNotFoundException.class, () -> contactService.getById(testId));
    }

    @Test
    void listAll_returnsAllContacts() {
        when(contactRepository.findAll()).thenReturn(List.of(testContact));
        List<ContactResponse> result = contactService.listAll();
        assertEquals(1, result.size());
        assertEquals(testId, result.get(0).id());
    }

    @Test
    void delete_deletesWhenExists() {
        when(contactRepository.existsById(testId)).thenReturn(true);
        doNothing().when(contactRepository).deleteById(testId);
        contactService.delete(testId);
        verify(contactRepository).deleteById(testId);
    }

    @Test
    void update_updatesContactAndPublishesEvent() {
        CreateContactRequest req = new CreateContactRequest(
            "Updated", "Name", "updated@vieterp.com", "0999999999",
            "0909998888", "Senior Developer", "Engineering", "https://new-avatar.url",
            "Updated notes", null, Contact.ContactStatus.ACTIVE, null, "VN", null, 75, "owner-456");

        when(contactRepository.findById(testId)).thenReturn(Optional.of(testContact));
        when(contactRepository.save(any(Contact.class))).thenReturn(testContact);

        ContactResponse resp = contactService.update(testId, req);

        assertNotNull(resp);
        verify(contactRepository).save(any(Contact.class));
        assertTrue(eventPublisher.wasPublishUpdatedCalled());
    }
}
