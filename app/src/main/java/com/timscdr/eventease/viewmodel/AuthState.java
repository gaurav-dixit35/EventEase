package com.timscdr.eventease.viewmodel;

public final class AuthState {
    public enum Status { IDLE, LOADING, SIGNED_IN, SIGNED_OUT, ERROR }
    public enum Error { NONE, NAME, ROLL, COURSE, YEAR, EMAIL, COLLEGE, AUTH_FAILED, DUPLICATE, STORAGE }
    public final Status status;
    public final Error error;
    public final long studentId;

    public AuthState(Status status, Error error, long studentId) {
        this.status = status;
        this.error = error;
        this.studentId = studentId;
    }

    public static AuthState of(Status status) { return new AuthState(status, Error.NONE, 0); }
}
