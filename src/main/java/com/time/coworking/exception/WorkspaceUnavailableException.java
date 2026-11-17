package com.time.coworking.exception;

public class WorkspaceUnavailableException extends RuntimeException {
    public WorkspaceUnavailableException(String message) {
        super(message);
    }
}