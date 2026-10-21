package com.timscdr.eventease.repository;

import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.concurrent.CompletableFuture;

/** Checks the organizer allow-list document in an Auth session isolated from student sessions. */
final class FirebaseOrganizerAuthRepository implements OrganizerAuthRepository {
    private final FirebaseAuth auth;
    private final FirebaseFirestore database;
    private final SharedPreferences preferences;
    FirebaseOrganizerAuthRepository(FirebaseAuth auth, FirebaseFirestore database, SharedPreferences preferences) {
        this.auth = auth; this.database = database; this.preferences = preferences;
    }
    @Override public CompletableFuture<OrganizerAccount> signIn(String email, String password) {
        String cleanEmail = email == null ? "" : email.trim();
        return FirebaseTask.future(auth.signInWithEmailAndPassword(cleanEmail, password == null ? "" : password))
                .thenCompose(result -> authorized(result.getUser()));
    }
    @Override public CompletableFuture<OrganizerAccount> restoreSession() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.isAnonymous()) return CompletableFuture.completedFuture(null);
        // A temporary Firestore/network failure must not erase a valid Firebase
        // session. The caller can show the login/retry path and restore it later.
        return authorized(user);
    }
    private CompletableFuture<OrganizerAccount> authorized(FirebaseUser user) {
        if (user == null) return failed(new IllegalStateException("Organizer authentication failed."));
        return FirebaseTask.future(database.collection("organizers").document(user.getUid()).get())
                .thenApply(doc -> {
                    if (!doc.exists() || !Boolean.TRUE.equals(doc.getBoolean("active"))) {
                        auth.signOut();
                        clearOrganizerDetails();
                        throw new java.util.concurrent.CompletionException(
                                new RepositoryException(RepositoryException.Reason.AUTH_FAILED,
                                        "This account has not been granted organizer access."));
                    }
                    String name = nonempty(doc.getString("name"), user.getDisplayName(), "Event Organizer");
                    String email = nonempty(doc.getString("email"), user.getEmail(), "");
                    String college = nonempty(doc.getString("collegeName"), "TIMSCDR", "TIMSCDR");
                    preferences.edit().putString("organizer_name", name).putString("organizer_email", email)
                            .putString("organizer_college", college).putString("organizer_uid", user.getUid()).apply();
                    return new OrganizerAccount(user.getUid(), name, email, college);
                });
    }
    @Override public CompletableFuture<Void> signOut() {
        auth.signOut();
        clearOrganizerDetails();
        // Signing out must work even when the Firebase project has not enabled
        // Anonymous Auth. Student sync will request an anonymous identity lazily.
        return CompletableFuture.completedFuture(null);
    }
    @Override public String currentDisplayName() { return preferences.getString("organizer_name", "Event Organizer"); }
    @Override public boolean isConfigured() { return true; }

    private static String nonempty(String value, String fallback, String finalValue) {
        if (value != null && !value.trim().isEmpty()) return value.trim();
        if (fallback != null && !fallback.trim().isEmpty()) return fallback.trim();
        return finalValue;
    }
    private void clearOrganizerDetails() {
        preferences.edit().remove("organizer_uid").remove("organizer_email")
                .remove("organizer_name").remove("organizer_college").apply();
    }
    private static <T> CompletableFuture<T> failed(Throwable error) {
        CompletableFuture<T> result = new CompletableFuture<>(); result.completeExceptionally(error); return result;
    }
}
