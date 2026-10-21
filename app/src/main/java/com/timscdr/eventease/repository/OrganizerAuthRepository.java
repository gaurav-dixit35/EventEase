package com.timscdr.eventease.repository;

import java.util.concurrent.CompletableFuture;

/** Organizer authentication is cloud verified and independent from the student profile session. */
public interface OrganizerAuthRepository {
    CompletableFuture<OrganizerAccount> signIn(String email, String password);
    CompletableFuture<OrganizerAccount> restoreSession();
    CompletableFuture<Void> signOut();
    String currentDisplayName();
    boolean isConfigured();
}
