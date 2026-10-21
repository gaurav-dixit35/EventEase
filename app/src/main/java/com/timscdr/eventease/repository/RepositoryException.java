package com.timscdr.eventease.repository;

/** Stable error categories so ViewModels never need to catch SQLite exceptions. */
public final class RepositoryException extends RuntimeException {
    public enum Reason { INVALID_INPUT, NOT_FOUND, CONFLICT, STORAGE, AUTH_FAILED, ALREADY_EXISTS }

    private final Reason reason;

    public RepositoryException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public RepositoryException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
