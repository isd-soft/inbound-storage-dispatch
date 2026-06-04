package com.isd.wms.exception;

public class ReplenishmentTaskNotFoundException extends RuntimeException {
    public ReplenishmentTaskNotFoundException(Long replenishmentTaskId) {
        super("Replenishment task not found with id: " + replenishmentTaskId);
    }
}

