package com.timscdr.eventease.repository;

import java.util.concurrent.CompletableFuture;

final class UnavailableOrganizerAuthRepository implements OrganizerAuthRepository {
    private static final String MESSAGE = "Firebase is not configured for this project.";
    @Override public CompletableFuture<OrganizerAccount> signIn(String email, String password) {
        CompletableFuture<OrganizerAccount> result = new CompletableFuture<>();
        result.completeExceptionally(new IllegalStateException(MESSAGE));
        return result;
    }
    @Override public CompletableFuture<OrganizerAccount> restoreSession() {
        return CompletableFuture.completedFuture(null);
    }
    @Override public CompletableFuture<Void> signOut() { return CompletableFuture.completedFuture(null); }
    @Override public String currentDisplayName() { return "Event Organizer"; }
    @Override public boolean isConfigured() { return false; }
}
