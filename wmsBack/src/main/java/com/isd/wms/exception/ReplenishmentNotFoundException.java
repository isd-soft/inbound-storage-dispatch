package com.isd.wms.exception;

public class ReplenishmentNotFoundException extends RuntimeException {
    public ReplenishmentNotFoundException(Long replenishmentTaskId) {
        super("Replenishment task not found with id: " + replenishmentTaskId);
    }
}

