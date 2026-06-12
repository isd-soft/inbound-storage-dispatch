package com.isd.wms.exception;

public class DuplicateBarcodeException extends RuntimeException {
    public DuplicateBarcodeException(String code) {
        super("Location code '" + code + "' is already in use");
    }
}
