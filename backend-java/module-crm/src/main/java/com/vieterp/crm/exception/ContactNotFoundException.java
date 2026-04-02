package com.vieterp.crm.exception;

import java.util.UUID;

public class ContactNotFoundException extends RuntimeException {
    public ContactNotFoundException(UUID id) {
        super("Contact not found: " + id);
    }
}
