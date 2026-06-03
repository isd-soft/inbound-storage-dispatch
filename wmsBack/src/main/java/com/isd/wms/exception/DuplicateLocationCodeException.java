package com.isd.wms.exception;

public class DuplicateLocationCodeException extends RuntimeException {
    public DuplicateLocationCodeException(String code) {
        super("Location code '" + code + "' is already in use");
    }
}
