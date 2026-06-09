package com.isd.wms.exception;

public class ProcessesNotFoundException extends RuntimeException {
    public ProcessesNotFoundException(Long taskId) {
        super("Processes not found of task with id: " + taskId);
    }
}
