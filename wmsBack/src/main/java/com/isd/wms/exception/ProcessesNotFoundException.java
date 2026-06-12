package com.isd.wms.exception;

public class ProcessesNotFoundException extends RuntimeException {
    public ProcessesNotFoundException(Long taskId) {
        super("Processes not found of task with id: " + taskId);
    }
    public ProcessesNotFoundException(String operator) {
        super("Processes not found for operator with username: " + operator);
    }
}
