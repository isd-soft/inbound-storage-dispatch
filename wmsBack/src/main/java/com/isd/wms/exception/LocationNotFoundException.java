package com.isd.wms.exception;

public class LocationNotFoundException extends RuntimeException {
    public LocationNotFoundException(Long id) {
        super("Location with ID " + id + " not found");
    }
}
