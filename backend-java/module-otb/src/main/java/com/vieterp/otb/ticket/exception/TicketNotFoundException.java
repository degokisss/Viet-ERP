package com.vieterp.otb.ticket.exception;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String id) {
        super("Ticket not found with id: " + id);
    }
}
