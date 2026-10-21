package com.timscdr.eventease.repository;

import android.content.Context;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.concurrent.CompletableFuture;

public final class FirebaseEnvironment {
    private static final String ORGANIZER_APP_NAME = "eventease_organizer";
    private static CompletableFuture<FirebaseUser> studentSignIn;
    private FirebaseEnvironment() { }
    public static boolean isConfigured(Context context) {
        try { return !FirebaseApp.getApps(context.getApplicationContext()).isEmpty(); }
        catch (RuntimeException unavailable) { return false; }
    }
    static FirebaseAuth auth() { return FirebaseAuth.getInstance(); }

    /**
     * Organizer credentials must not replace the device's anonymous student
     * identity. A second FirebaseApp keeps both Firebase Auth sessions alive
     * independently while still connecting to the same Firebase project.
     */
    static synchronized FirebaseApp organizerApp(Context context) {
        for (FirebaseApp app : FirebaseApp.getApps(context.getApplicationContext())) {
            if (ORGANIZER_APP_NAME.equals(app.getName())) return app;
        }
        FirebaseApp defaultApp = FirebaseApp.getInstance();
        return FirebaseApp.initializeApp(context.getApplicationContext(),
                defaultApp.getOptions(), ORGANIZER_APP_NAME);
    }

    static synchronized CompletableFuture<FirebaseUser> ensureStudentIdentity() {
        FirebaseAuth auth = auth();
        FirebaseUser current = auth.getCurrentUser();
        if (current != null && current.isAnonymous()) return CompletableFuture.completedFuture(current);
        if (current != null) {
            CompletableFuture<FirebaseUser> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalStateException("Sign out from the organizer portal first."));
            return failed;
        }
        // Several repositories can request the student identity as the
        // dashboard starts. Share one in-flight anonymous sign-in so parallel
        // requests cannot create competing Firebase identities.
        if (studentSignIn != null) return studentSignIn;
        CompletableFuture<FirebaseUser> request = FirebaseTask.future(auth.signInAnonymously())
                .thenApply(result -> result.getUser());
        studentSignIn = request;
        request.whenComplete((user, failure) -> {
            synchronized (FirebaseEnvironment.class) {
                if (studentSignIn == request) studentSignIn = null;
            }
        });
        return request;
    }
}
