package com.isd.wms.exception;

public class DuplicateLocationCodeException extends RuntimeException {

    public DuplicateLocationCodeException(String locationCode) {
        super("Location code '" + locationCode + "' already exists. Please choose a different code.");
    }

}
