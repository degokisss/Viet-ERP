package com.vieterp.mrp.exception;

import java.util.UUID;

public class BomNotFoundException extends RuntimeException {
    public BomNotFoundException(UUID id) {
        super("BOM header not found: " + id);
    }
}
